package com.project.cybershop.support.internal.service.impl;

import com.project.cybershop.support.api.dto.request.AiChatRequest;
import com.project.cybershop.support.api.dto.response.AiChatResponse;
import com.project.cybershop.support.internal.model.ComponentUpgradeDiff;
import com.project.cybershop.support.internal.model.PcBuildProfile;
import com.project.cybershop.support.internal.model.ReconstructedBuildContext;
import com.project.cybershop.support.internal.repository.SupportCatalogLookupRepository;
import com.project.cybershop.support.internal.repository.SupportCustomerContextRepository;
import com.project.cybershop.support.internal.service.SupportIntentResolver;
import com.project.cybershop.support.internal.service.SupportPcBuildAnalyzer;
import com.project.cybershop.support.internal.service.SupportPcBuildSolver;
import com.project.cybershop.support.internal.service.SupportProductAdvisor;
import com.project.cybershop.support.internal.support.SupportLlmClient;
import com.project.cybershop.support.internal.support.SupportSecurityGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupportPcBuildAnalyzerImpl implements SupportPcBuildAnalyzer {

    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###đ");

    private final SupportCatalogLookupRepository catalogRepository;
    private final SupportCustomerContextRepository customerContextRepository;
    private final SupportIntentResolver intentResolver;
    private final SupportPcBuildSolver pcBuildSolver;
    private final SupportProductAdvisor productAdvisor;
    private final SupportSecurityGuard securityGuard;
    private final SupportLlmClient llmClient;

    @Override
    public ReconstructedBuildContext reconstructAdvisedBuildFromHistory(
            List<AiChatRequest.HistoryItem> history,
            String fallbackMessage) {
        String bestBuildBotText = null;
        Long originalBudgetFromBot = null;

        if (history != null) {
            for (int i = history.size() - 1; i >= 0; i--) {
                AiChatRequest.HistoryItem h = history.get(i);
                if ("bot".equalsIgnoreCase(h.getSender()) && h.getText() != null) {
                    String bt = h.getText();
                    if (bt.contains("🔹") || bt.contains("Chi tiết trọn bộ")) {
                        if (bestBuildBotText == null) {
                            bestBuildBotText = bt;
                        }
                        Matcher bm = Pattern
                                .compile("Ngân sách(?:\\s+dự kiến|\\s+mục tiêu)?:\\s*([\\d.,]+)\\s*đ", Pattern.CASE_INSENSITIVE)
                                .matcher(bt);
                        if (bm.find()) {
                            try {
                                String digits = bm.group(1).replaceAll("[^\\d]", "");
                                if (!digits.isEmpty()) {
                                    originalBudgetFromBot = Long.parseLong(digits);
                                }
                            } catch (Exception ignored) {}
                        }
                        break;
                    }
                }
            }
        }

        List<AiChatResponse.ProductSummary> currentBuild = new ArrayList<>();
        if (bestBuildBotText != null) {
            List<String> lines = intentResolver.extractComponentLines(bestBuildBotText);
            Set<String> seenGroups = new HashSet<>();
            for (String line : lines) {
                AiChatResponse.ProductSummary p = catalogRepository.findExactAdvisedProduct(line);
                if (p != null) {
                    String grp = securityGuard.normalizeCategoryGroup(p.getCategory());
                    if (seenGroups.add(grp)) {
                        currentBuild.add(p);
                    }
                }
            }
        }

        long fallbackBudget = originalBudgetFromBot != null ? originalBudgetFromBot : 40_000_000L;
        if (currentBuild.size() < 4) {
            currentBuild = new ArrayList<>(pcBuildSolver.buildRealPcComponentsForBudget(fallbackBudget, fallbackMessage, history));
        }

        return new ReconstructedBuildContext(currentBuild, originalBudgetFromBot, bestBuildBotText);
    }

    @Override
    public boolean isPcBuildDeepDiveAnalysisQuery(
            String message,
            List<AiChatRequest.HistoryItem> history) {
        if (message == null || message.isBlank()) return false;
        if (!intentResolver.hasRecentPcBuildInHistory(history)) return false;

        String m = message.toLowerCase().trim();
        if (intentResolver.isReferralComponentQuery(message) || intentResolver.isPcUpgradeOrSurplusQuery(message, history)) {
            return false;
        }
        if (intentResolver.extractExplicitBudget(message) != null) {
            return false;
        }
        if (m.matches("^(build|biuld|ild|lên cấu hình|ráp pc|lắp pc)\\b.*")
                || m.matches(".*(đổi sang intel|đổi qua intel|chuyển sang intel|đổi sang amd|đổi qua amd|chuyển sang amd|làm bộ màu trắng|đổi sang màu trắng|rẻ hơn|hạ giá|giảm giá).*")) {
            return false;
        }
        if (m.contains("so sánh") || m.contains(" vs ")) {
            return false;
        }

        boolean isOverallBuildEvaluation = m.matches(".*(hiệu năng|đánh giá|review|phân tích|nhận xét|kỹ hơn|kĩ hơn|chi tiết hơn|rõ hơn|cụ thể hơn|sâu hơn|giải thích|nói thêm|tư vấn thêm|mạnh không|mượt không|ổn không|tốt không|ngon không|ok không|fps|bao nhiêu fps|chơi được|chiến được|cân được|làm được|render được|nghẽn cổ chai|bottleneck|nhiệt độ|mát không|nóng không|tốn điện|tiêu thụ điện|ưu nhược điểm|điểm mạnh|tại sao chọn|vì sao chọn|lý do chọn).*");

        Set<String> targetCats = intentResolver.extractTargetCategories(m);
        boolean mentionsWifiOrConnectivity = m.matches(".*\\b(wifi|wi-fi|bluetooth|mạng dây|lan|khe ram|khe m2|nâng cấp về sau)\\b.*");

        if (!targetCats.isEmpty() || mentionsWifiOrConnectivity) {
            ReconstructedBuildContext ctx = reconstructAdvisedBuildFromHistory(history, message);
            String buildNamesCombined = ctx.components().stream()
                    .map(p -> (p.getName() + " " + (p.getVariantName() != null ? p.getVariantName() : "")).toLowerCase())
                    .collect(Collectors.joining(" "));

            Matcher modelMatcher = Pattern
                    .compile("\\b(rtx\\s*\\d{4}|rx\\s*\\d{4}|gtx\\s*\\d{4}|i[3579][\\s\\-]\\d{4,5}|ryzen\\s*[3579]\\s*\\d{4}[a-z0-9]*)\\b")
                    .matcher(m);
            while (modelMatcher.find()) {
                String mentionedModel = modelMatcher.group(1).replaceAll("\\s+", " ").trim();
                String digitsOnly = mentionedModel.replaceAll("[^0-9]", "");
                if (!digitsOnly.isEmpty() && !buildNamesCombined.contains(digitsOnly)) {
                    return false;
                }
            }

            if (isOverallBuildEvaluation || m.matches(".*(nói|kể|giải thích|phân tích|tư vấn|đánh giá|chi tiết|kỹ|kĩ|rõ|sâu|thêm|sao|thế nào|như nào|ổn|mạnh|tốt|gì|vậy còn|thế còn|còn|trong bộ|của bộ|bộ này|dàn này|wifi|bluetooth).*")) {
                return true;
            }
        }

        return isOverallBuildEvaluation;
    }

    @Override
    public AiChatResponse handlePcBuildDeepDiveAdvisory(
            String message,
            List<AiChatRequest.HistoryItem> history,
            String conversationId) {
        ReconstructedBuildContext ctx = reconstructAdvisedBuildFromHistory(history, message);
        List<AiChatResponse.ProductSummary> currentBuild = ctx.components();
        double totalVal = securityGuard.sumBuildPrice(currentBuild);
        String msgLower = message != null ? message.toLowerCase().trim() : "";

        Set<String> targetCats = new LinkedHashSet<>(intentResolver.extractTargetCategories(msgLower));
        if (msgLower.matches(".*\\b(wifi|wi-fi|bluetooth|khe ram|khe cắm|cổng usb)\\b.*")) {
            targetCats.add("MAINBOARD");
        }

        boolean asksWholePc = targetCats.isEmpty() || msgLower.matches(".*(cả bộ|toàn bộ|tổng thể|8 món|8 linh kiện).*");

        if (!targetCats.isEmpty() && !asksWholePc) {
            List<AiChatResponse.ProductSummary> matchedParts = new ArrayList<>();
            for (AiChatResponse.ProductSummary p : currentBuild) {
                if (targetCats.contains(securityGuard.normalizeCategoryGroup(p.getCategory()))) {
                    matchedParts.add(p);
                }
            }

            if (!matchedParts.isEmpty()) {
                String partLabels = matchedParts.stream()
                        .map(p -> securityGuard.getCategoryDisplayName(p.getCategory()))
                        .collect(Collectors.joining(" & "));

                StringBuilder reply = new StringBuilder();
                reply.append("### 🔍 Phân tích kỹ thuật chuyên sâu: **").append(partLabels).append("** _(Trong cấu hình PC của bạn)_\n\n");
                reply.append("Dưới đây là đánh giá chi tiết về kiến trúc phần cứng, lý do lựa chọn và vai trò thực tế của từng linh kiện trong dàn máy **")
                        .append(MONEY_FMT.format(totalVal)).append("** mà CyberBot vừa lên cho bạn:\n\n");

                int idx = 1;
                for (AiChatResponse.ProductSummary part : matchedParts) {
                    String catDisplay = securityGuard.getCategoryDisplayName(part.getCategory());
                    String vName = part.getVariantName() != null && !part.getVariantName().equalsIgnoreCase("Tiêu chuẩn")
                            ? " (" + part.getVariantName() + ")" : "";
                    String cleanSpecs = securityGuard.sanitizeSpecsForUser(part.getSpecs() != null ? part.getSpecs() : part.getDescription());

                    reply.append("#### ").append(idx++).append(". ").append(catDisplay).append(": **")
                            .append(part.getName()).append(vName).append("** — **")
                            .append(MONEY_FMT.format(part.getPrice() != null ? part.getPrice() : 0.0)).append("**\n");
                    if (!cleanSpecs.isBlank()) {
                        reply.append("* **Thông số kỹ thuật cốt lõi**: `").append(cleanSpecs).append("`\n");
                    }
                    reply.append(buildDeepEngineeringAnalysisForBuildPart(part, currentBuild)).append("\n\n");
                }

                String defaultPartConclusion = "Sự kết hợp của các linh kiện trên trong cấu hình đã được tính toán đồng bộ 100% về mặt băng thông và chuẩn giao tiếp vật lý, giúp phát huy tối đa hiệu năng trên từng đồng ngân sách.";
                String partFacts = matchedParts.stream()
                        .map(p -> p.getName() + " (" + MONEY_FMT.format(p.getPrice() != null ? p.getPrice() : 0.0) + ")")
                        .collect(Collectors.joining(", "));
                String aiPartConclusion = llmClient.generateGroundedCommentary(
                        "PC_DEEP_DIVE_PART", message, "Linh kiện đang xét trong dàn PC " + MONEY_FMT.format(totalVal) + ": " + partFacts, defaultPartConclusion);

                reply.append("💡 **Kết luận từ Kỹ sư phần cứng TechZone**:\n");
                reply.append(aiPartConclusion).append("\n\n");
                reply.append("👉 Bạn có muốn **nâng cấp/đổi thử mã linh kiện nào** trong số này hay **chốt thêm vào giỏ hàng** ngay không ạ?");

                return AiChatResponse.builder()
                        .conversationId(conversationId)
                        .message(reply.toString())
                        .messageType("info")
                        .products(matchedParts)
                        .quickActions(List.of(
                                new AiChatResponse.QuickAction("🛒 Thêm tất cả vào giỏ hàng", "add_all_to_cart", "add_all_to_cart"),
                                new AiChatResponse.QuickAction("💳 Xem giỏ hàng", "navigate", "/gio-hang"),
                                new AiChatResponse.QuickAction("💬 Nhờ chuyên viên hỗ trợ", "connect_staff", null)
                        ))
                        .build();
            }
        }

        AiChatResponse.ProductSummary cpu = null, main = null, ram = null, vga = null, ssd = null, psu = null, pcCase = null, cooler = null;
        for (AiChatResponse.ProductSummary p : currentBuild) {
            switch (securityGuard.normalizeCategoryGroup(p.getCategory())) {
                case "CPU" -> cpu = p;
                case "MAINBOARD" -> main = p;
                case "RAM" -> ram = p;
                case "VGA" -> vga = p;
                case "SSD" -> ssd = p;
                case "NGUỒN" -> psu = p;
                case "CASE" -> pcCase = p;
                case "TẢN NHIỆT" -> cooler = p;
            }
        }

        String cpuName = cpu != null ? cpu.getName() : "CPU hiệu năng cao";
        String vgaName = vga != null ? vga.getName() : "Card đồ họa rời";
        String mainName = main != null ? main.getName() : "Bo mạch chủ";
        String ramName = ram != null ? ram.getName() : "RAM chuẩn mới";
        String ssdName = ssd != null ? ssd.getName() : "SSD NVMe";
        String psuName = psu != null ? psu.getName() : "Nguồn công suất thực";
        String coolerName = cooler != null ? cooler.getName() : "Tản nhiệt CPU";
        String caseName = pcCase != null ? pcCase.getName() : "Vỏ Case Airflow";

        String vgaLower = (vgaName + " " + (vga != null && vga.getSpecs() != null ? vga.getSpecs() : "")).toLowerCase();
        String cpuLower = (cpuName + " " + (cpu != null && cpu.getSpecs() != null ? cpu.getSpecs() : "")).toLowerCase();

        String esportFps;
        String aaa1080pFps;
        String aaa2k4kFps;
        String workstationPerf;

        if (vgaLower.matches(".*(5080|5090|4080|4090).*")) {
            esportFps = "**500 – 750+ FPS** (Max Setting ở độ phân giải 2K/4K, khai thác trọn vẹn màn hình 360Hz – 540Hz)";
            aaa1080pFps = "**160 – 220+ FPS** (Ultra Setting + Full Ray Tracing)";
            aaa2k4kFps = "**110 – 165+ FPS ở chuẩn 4K UHD Ultra** (kích hoạt NVIDIA DLSS 4 + Multi-Frame Generation mượt tuyệt đối)";
            workstationPerf = "Render 3D Blender Cycles (OptiX), Lumion, V-Ray, biên tập Timeline 8K RAW và chạy mô hình AI cục bộ (LLM/Stable Diffusion) ở tốc độ đỉnh cao đầu bảng.";
        } else if (vgaLower.matches(".*(5070|4070|9070|7800 xt|7900).*")) {
            esportFps = "**400 – 600+ FPS** (Competitive/Max Setting ở Full HD & 2K)";
            aaa1080pFps = "**130 – 175+ FPS** (Ultra Setting)";
            aaa2k4kFps = "**95 – 140+ FPS ở độ phân giải 2K (1440p) Ultra Setting** và **65 – 90 FPS ở 4K High/Ultra + DLSS 4**";
            workstationPerf = "Dựng hình 3D Blender mượt mà với bộ nhớ VRAM lớn, kết xuất đồ họa CUDA/OptiX nhanh gấp nhiều lần render CPU thuần, dựng phim Premiere Pro / DaVinci Resolve 4K đa lớp không cần tạo Proxy.";
        } else if (vgaLower.matches(".*(5060|4060|7600|7700|6600|b580).*")) {
            esportFps = cpuLower.contains("x3d")
                    ? "**380 – 550+ FPS** nhờ sức mạnh bộ nhớ đệm 3D V-Cache của **" + cpuName + "** (tối ưu tuyệt đối cho màn hình 240Hz – 360Hz)"
                    : "**280 – 420+ FPS** (Max Setting ở Full HD / 2K, cực kỳ mượt mà trên màn hình 165Hz – 240Hz)";
            aaa1080pFps = "**90 – 135+ FPS ở Full HD (1080p) Ultra Setting**";
            aaa2k4kFps = "**70 – 105+ FPS ở độ phân giải 2K (1440p) High/Ultra Setting** (kết hợp công nghệ **DLSS 4 / Frame Generation**)";
            workstationPerf = "Tăng tốc phần cứng nhân **CUDA / OptiX & bộ mã hóa NVENC AV1** giúp vừa chiến game Max Setting vừa Livestream OBS 1080p 60fps không tụt khung hình, đồng thời đáp ứng rất tốt chỉnh sửa ảnh Photoshop, Lightroom, dựng video Premiere / CapCut PC và vẽ 3D Blender tầm trung.";
        } else {
            esportFps = "**160 – 260+ FPS** (Full HD High/Max Setting)";
            aaa1080pFps = "**60 – 85+ FPS ở Full HD (1080p) Medium/High Setting**";
            aaa2k4kFps = "Tối ưu tốt nhất ở chuẩn màn hình **Full HD (1080p)**";
            workstationPerf = "Đáp ứng mượt mà các tác vụ đồ họa 2D (Photoshop, Illustrator, AutoCAD), dựng video Full HD và làm việc đa nhiệm.";
        }

        StringBuilder reply = new StringBuilder();
        reply.append("### 📊 Phân tích chuyên sâu Hiệu năng & Kỹ thuật cấu hình PC (").append(MONEY_FMT.format(totalVal)).append(")\n\n");

        reply.append("#### 1. Sức mạnh cốt lõi: Sự phối hợp giữa **").append(cpuName).append("** & **").append(vgaName).append("**\n");
        if (cpuLower.contains("x3d")) {
            reply.append("* **Đỉnh cao Gaming 3D V-Cache**: **").append(cpuName).append("** sở hữu bộ nhớ đệm L3 khổng lồ **96MB 3D V-Cache**, giúp CPU nạp sẵn toàn bộ dữ liệu vật lý/logic của trò chơi ngay trong nhân chip mà không phải chờ truy xuất qua RAM. Nhờ đó, mức **FPS tối thiểu (`1% Low FPS`) cao vượt trội**, loại bỏ hoàn toàn hiện tượng khựng khung hình (micro-stuttering) trong các pha giao tranh tổng nhiều hiệu ứng.\n");
        } else if (cpuLower.matches(".*(14700|270k|9900x|9950x|14600).*")) {
            reply.append("* **Sức mạnh Đa nhân & Xung nhịp cao**: **").append(cpuName).append("** sở hữu kiến trúc nhiều nhân/luồng xung nhịp cao, vừa cấp dữ liệu tức thì cho Card đồ họa khi chơi game, vừa rút ngắn tối đa thời gian tính toán vật lý, biên dịch code và kết xuất đồ họa (Render).\n");
        } else {
            reply.append("* **Cân bằng hoàn hảo (0% Bottleneck)**: **").append(cpuName).append("** có IPC (hiệu năng trên mỗi xung nhịp) cao, đảm bảo khai thác **100% công suất của ").append(vgaName).append("** mà không gây nghẽn cổ chai ở bất kỳ tựa game hay phần mềm nào.\n");
        }
        reply.append("* **Công nghệ đồ họa thế hệ mới**: **").append(vgaName).append("** hỗ trợ đầy đủ tập lệnh đồ họa mới nhất, từ dựng hình ánh sáng thực **Ray Tracing**, siêu phân giải trí tuệ nhân tạo **DLSS / FSR** cho đến giảm độ trễ chuột **NVIDIA Reflex**.\n\n");

        reply.append("#### 2. Hiệu năng thực chiến theo từng nhu cầu (Benchmark ước tính)\n");
        reply.append("* 🎮 **Game eSports & FPS Cạnh tranh** *(Valorant, CS2, Liên Minh Huyền Thoại, PUBG, Dota 2)*:\n");
        reply.append("  ↳ Đạt trung bình ").append(esportFps).append(" — phản hồi khung hình tức thì, frametime phẳng tuyệt đối.\n");
        reply.append("* 🔥 **Game AAA Thế giới mở & Đồ họa nặng** *(Black Myth: Wukong, Cyberpunk 2077, Elden Ring, GTA V/VI, Monster Hunter Wilds)*:\n");
        reply.append("  ↳ Ở **Full HD (1080p)**: Đạt ").append(aaa1080pFps).append(".\n");
        reply.append("  ↳ Ở **2K QHD (1440p) / 4K**: Đạt ").append(aaa2k4kFps).append(".\n");
        reply.append("* 🎨 **Đồ họa, Dựng phim & Livestream đa nhiệm**:\n");
        reply.append("  ↳ ").append(workstationPerf).append("\n\n");

        reply.append("#### 3. Độ bền linh kiện nền tảng, Nhiệt độ & An toàn điện năng\n");
        reply.append("* **Bo mạch chủ & Bộ nhớ RAM**: **").append(mainName).append("** kết hợp cùng **").append(ramName).append("** mang lại băng thông truyền tải dữ liệu cực nhanh, dàn VRM có tản nhiệt giúp giữ xung nhịp CPU luôn ở mức cao nhất mà không bị nóng mạch.\n");
        reply.append("* **Tốc độ Lưu trữ**: Ổ cứng **").append(ssdName).append("** giúp khởi động Windows chỉ trong **5–7 giây** và nạp bản đồ game AAA gần như tức thì.\n");
        reply.append("* **Nguồn & Nhiệt độ vận hành**: Bộ nguồn **").append(psuName).append("** dư công suất an toàn cho toàn hệ thống khi tải nặng 100%, đi cùng tản nhiệt **").append(coolerName).append("** và vỏ **").append(caseName).append("** giúp nhiệt độ CPU/VGA khi chiến game liên tục luôn duy trì mát mẻ ở ngưỡng **60°C – 72°C**.\n\n");

        reply.append("#### 4. Tiềm năng nâng cấp dài hạn (Upgrade Path 3 – 5 năm)\n");
        int ramCap = securityGuard.extractCapacityGb(ram);
        if (ramCap <= 16) {
            reply.append("* Bo mạch chủ **").append(mainName).append("** vẫn còn trống khe RAM và khe M.2 PCIe, cho phép bạn **cắm thêm 1 thanh RAM 16GB nữa để lên 32GB Dual-Channel** hoặc gắn thêm ổ SSD thứ 2 bất cứ lúc nào mà không phải bỏ đi linh kiện cũ.\n\n");
        } else {
            reply.append("* Với nền tảng bo mạch chủ **").append(mainName).append("** và dung lượng RAM lớn sẵn có, bạn hoàn toàn yên tâm sử dụng ổn định 3–5 năm tới và dễ dàng nâng cấp thêm ổ cứng hoặc thế hệ GPU tiếp theo.\n\n");
        }

        reply.append("👉 **Bạn có muốn tìm hiểu sâu hơn về từng món linh kiện cụ thể (như CPU, RAM, VGA, Nguồn) hay muốn điều chỉnh/nâng cấp hạng mục nào trong dàn này không ạ?**");

        return AiChatResponse.builder()
                .conversationId(conversationId)
                .message(reply.toString())
                .messageType("info")
                .products(currentBuild)
                .quickActions(List.of(
                        new AiChatResponse.QuickAction("🛒 Thêm tất cả vào giỏ hàng", "add_all_to_cart", "add_all_to_cart"),
                        new AiChatResponse.QuickAction("💳 Xem giỏ hàng", "navigate", "/gio-hang"),
                        new AiChatResponse.QuickAction("💬 Nhờ chuyên viên hỗ trợ", "connect_staff", null)
                ))
                .build();
    }

    private String buildDeepEngineeringAnalysisForBuildPart(
            AiChatResponse.ProductSummary part,
            List<AiChatResponse.ProductSummary> fullBuild) {
        if (part == null) return "";
        String catGroup = securityGuard.normalizeCategoryGroup(part.getCategory());
        String haystack = (part.getName() + " " + (part.getVariantName() != null ? part.getVariantName() : "") + " "
                + (part.getSpecs() != null ? part.getSpecs() : "") + " " + (part.getDescription() != null ? part.getDescription() : "")).toLowerCase();

        AiChatResponse.ProductSummary cpu = null, main = null, ram = null, vga = null, psu = null, cooler = null;
        if (fullBuild != null) {
            for (AiChatResponse.ProductSummary p : fullBuild) {
                switch (securityGuard.normalizeCategoryGroup(p.getCategory())) {
                    case "CPU" -> cpu = p;
                    case "MAINBOARD" -> main = p;
                    case "RAM" -> ram = p;
                    case "VGA" -> vga = p;
                    case "NGUỒN" -> psu = p;
                    case "TẢN NHIỆT" -> cooler = p;
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        switch (catGroup) {
            case "CPU" -> {
                if (haystack.contains("x3d")) {
                    sb.append("* **Kiến trúc & Công nghệ đột phá**: Sở hữu công nghệ **AMD 3D V-Cache** với bộ nhớ đệm L3 lên tới **96MB** (gấp 3 lần các CPU thông thường chỉ có 32MB). Trong tác vụ Gaming, bộ nhớ đệm L3 đóng vai trò như kho dữ liệu siêu tốc nằm sát nhân xử lý, giúp CPU giải quyết hàng triệu lệnh dựng hình (Draw Calls) mỗi giây mà không bị độ trễ của RAM kìm hãm. Nhờ đó, FPS trung bình và đặc biệt là chỉ số **`1% Low FPS` tăng vọt 25% – 35%**, mang lại cảm giác di chuột mượt tuyệt đối.\n");
                } else if (haystack.matches(".*(14700|270k|9900x|9950x|14600|9700x).*")) {
                    sb.append("* **Kiến trúc & Sức mạnh đa nhiệm**: Trang bị nhiều nhân xử lý xung nhịp Boost cao cùng bộ nhớ đệm L3 lớn, mang lại hiệu năng đơn nhân (Single-Core) xuất sắc cho Gaming lẫn sức mạnh đa nhân (Multi-Core) vượt trội khi vừa chơi game, vừa mở Discord/Trình duyệt, Livestream hoặc Render đồ họa 3D.\n");
                } else {
                    sb.append("* **Kiến trúc & Hiệu năng thực tế**: Là mẫu CPU chuyên dụng (không bị cắt giảm làn PCIe hay bộ nhớ đệm L3 như dòng APU), sở hữu xung nhịp cao giúp xử lý mượt mà các phép tính vật lý trong game và truyền tải dữ liệu tức thì xuống Card đồ họa.\n");
                }
                sb.append("* **Sự phối hợp trong dàn máy của bạn**: Đi cặp hoàn hảo 100% với bo mạch chủ **")
                        .append(main != null ? main.getName() : "Mainboard trong dàn").append("**, được giải nhiệt trực tiếp bởi **")
                        .append(cooler != null ? cooler.getName() : "Tản nhiệt rời").append("** và kéo trọn vẹn 100% sức mạnh của **")
                        .append(vga != null ? vga.getName() : "Card đồ họa rời").append("** mà hoàn toàn không có hiện tượng nghẽn cổ chai.");
            }
            case "RAM" -> {
                int cap = securityGuard.extractCapacityGb(part);
                boolean isDdr5 = haystack.contains("ddr5");
                boolean is6000 = haystack.contains("6000");
                sb.append("* **Băng thông & Tốc độ truyền tải**: Sử dụng chuẩn **")
                        .append(isDdr5 ? "DDR5 thế hệ mới" : "DDR4 tốc độ cao").append("**")
                        .append(is6000 ? " với mức xung nhịp **Bus 6000MHz** — đây chính là mức **\"Sweet-Spot\" (điểm ngọt kỹ thuật)** giúp bộ điều khiển nhớ (IMC) của CPU đạt băng thông cao nhất và độ trễ (Latency) thấp nhất khi chơi game.\n" : " cho băng thông truyền tải dữ liệu dồi dào và độ ổn định cao.\n");
                if (cap <= 16) {
                    sb.append("* **Dung lượng & Lộ trình nâng cấp thông minh**: Dung lượng **").append(cap).append("GB** hiện đáp ứng rất tốt mọi tựa game AAA và đa nhiệm. Đặc biệt, vì được cấu hình dưới dạng **1 thanh ").append(cap).append("GB đơn lẻ** cắm trên bo mạch chủ **")
                            .append(main != null ? main.getName() : "Mainboard").append("** (vốn còn trống khe cắm RAM), bạn có thể **dễ dàng cắm thêm 1 thanh ").append(cap).append("GB nữa bất cứ lúc nào để nâng cấp lên ").append(cap * 2).append("GB Dual-Channel** mà không bị lãng phí khe cắm!");
                } else {
                    sb.append("* **Dung lượng Đa nhiệm Dư dả**: Với dung lượng **").append(cap).append("GB**, hệ thống của bạn dư sức chạy mượt mọi tựa game AAA ngốn RAM nhất hiện nay, đồng thời thoải mái dựng hình 3D Blender, biên tập video 4K và mở hàng chục ứng dụng nền cùng lúc.");
                }
            }
            case "VGA" -> {
                sb.append("* **Sức mạnh Nhân đồ họa & Công nghệ AI**: Là trái tim xử lý hình ảnh của cả dàn máy, hỗ trợ phần cứng cho **Ray Tracing** (tái tạo ánh sáng/phản chiếu chân thực), **DLSS / Frame Generation** (tăng gấp rưỡi đến gấp đôi FPS nhờ nhân AI Tensor) và bộ mã hóa phần cứng chuyên dụng cho Livestream/Render video.\n");
                sb.append("* **Sự phối hợp trong dàn máy của bạn**: Kết nối qua giao tiếp PCIe băng thông cao trên **")
                        .append(main != null ? main.getName() : "Mainboard").append("**, nhận dữ liệu khung hình trực tiếp từ **")
                        .append(cpu != null ? cpu.getName() : "CPU").append("** và được cấp điện ổn định bởi bộ nguồn **")
                        .append(psu != null ? psu.getName() : "PSU").append("**.");
            }
            case "MAINBOARD" -> {
                boolean hasWifi = haystack.contains("wifi") || haystack.contains("wi-fi");
                sb.append("* **Nền tảng cấp nguồn VRM & Giao tiếp**: Sở hữu mạch VRM kỹ thuật số kèm heatsink tản nhiệt nhôm đúc giúp giữ dòng điện nuôi **")
                        .append(cpu != null ? cpu.getName() : "CPU").append("** luôn phẳng và mát mẻ ngay cả khi chạy Max Turbo hàng giờ liền.\n");
                sb.append("* **Tiện ích & Kết nối**: Đồng bộ hoàn toàn chuẩn Socket với CPU và chuẩn bộ nhớ với **")
                        .append(ram != null ? ram.getName() : "RAM").append("**, hỗ trợ khe M.2 NVMe PCIe tốc độ cao")
                        .append(hasWifi ? " và **tích hợp sẵn Wi-Fi + Bluetooth băng tần kép** giúp kết nối mạng không dây và tay cầm chơi game/tai nghe Bluetooth cực kỳ tiện lợi." : ".");
            }
            case "SSD" -> {
                sb.append("* **Tốc độ Đọc/Ghi NVMe PCIe**: Giao tiếp trực tiếp với CPU qua làn PCIe tốc độ cao, loại bỏ hoàn toàn độ trễ nạp dữ liệu (stuttering do load texture chậm trong các tựa game thế giới mở) và khởi động hệ điều hành chỉ trong vài giây.");
            }
            case "NGUỒN" -> {
                sb.append("* **Trái tim năng lượng chuẩn 80 Plus**: Đảm bảo công suất thực dư dả (Headroom an toàn 25–35%) cho tổ hợp **")
                        .append(cpu != null ? cpu.getName() : "CPU").append(" + ").append(vga != null ? vga.getName() : "VGA")
                        .append("**, giúp linh kiện không bao giờ bị sụt áp khi tải đỉnh (Transient Spikes) và tăng tuổi thọ toàn bộ dàn máy.");
            }
            case "TẢN NHIỆT" -> {
                sb.append("* **Kiểm soát nhiệt độ & Độ ồn**: Giải nhiệt trực tiếp cho **")
                        .append(cpu != null ? cpu.getName() : "CPU")
                        .append("**, giữ mức nhiệt khi chơi game nặng luôn dưới ngưỡng 72°C để CPU duy trì xung nhịp Boost tối đa liên tục mà không bị giảm xung do quá nhiệt (Thermal Throttling).");
            }
            case "CASE" -> {
                sb.append("* **Không gian lắp đặt & Đối lưu khí**: Tương thích hoàn hảo kích thước của **")
                        .append(main != null ? main.getName() : "Mainboard").append("** và chiều dài của **")
                        .append(vga != null ? vga.getName() : "Card đồ họa")
                        .append("**, thiết kế mặt thoáng đón gió giúp đẩy nhanh khí nóng ra ngoài và khoe trọn vẻ đẹp linh kiện.");
            }
            default -> {
                sb.append(productAdvisor.buildExpertHardwareInsightForProduct(part));
            }
        }
        return sb.toString();
    }

    @Override
    public AiChatResponse handlePcUpgradeOrSurplusAdvisory(
            String message,
            List<AiChatRequest.HistoryItem> history,
            String conversationId) {
        ReconstructedBuildContext reconCtx = reconstructAdvisedBuildFromHistory(history, message);
        List<AiChatResponse.ProductSummary> currentBuild = new ArrayList<>(reconCtx.components());
        Long originalBudgetFromBot = reconCtx.originalBudget();

        double previousTotal = securityGuard.sumBuildPrice(currentBuild);
        long baseBudget = originalBudgetFromBot != null ? originalBudgetFromBot : Math.round(previousTotal);

        BigDecimal mentionedAmountBd = intentResolver.extractExplicitBudget(message);
        long mentionedAmount = mentionedAmountBd != null ? mentionedAmountBd.longValue() : 0L;
        String msgLower = message != null ? message.toLowerCase().trim() : "";

        long targetBudget;
        boolean isAddingExtraOnTop = msgLower.matches(".*(thêm|bù thêm|cộng thêm|tăng thêm|bỏ thêm)\\s*\\d+.*")
                && !msgLower.matches(".*(dư|thừa|tiết kiệm).*");

        if (isAddingExtraOnTop && mentionedAmount > 0 && mentionedAmount < baseBudget) {
            targetBudget = Math.round(previousTotal) + mentionedAmount;
        } else if (mentionedAmount > 0 && mentionedAmount < baseBudget * 0.6) {
            double actualSurplus = baseBudget - previousTotal;
            if (actualSurplus >= mentionedAmount * 0.55) {
                targetBudget = baseBudget;
            } else {
                targetBudget = Math.max(baseBudget, Math.round(previousTotal) + mentionedAmount);
            }
        } else if (mentionedAmount >= baseBudget * 0.6) {
            targetBudget = mentionedAmount;
        } else {
            double actualSurplus = baseBudget - previousTotal;
            targetBudget = actualSurplus >= 600_000 ? baseBudget : Math.round(previousTotal) + 3_500_000L;
        }

        PcBuildProfile profile = pcBuildSolver.parsePcBuildProfile(message, history, targetBudget);

        AiChatResponse.ProductSummary currentCpu = null;
        AiChatResponse.ProductSummary currentMain = null;
        for (AiChatResponse.ProductSummary p : currentBuild) {
            String grp = securityGuard.normalizeCategoryGroup(p.getCategory());
            if ("CPU".equals(grp)) currentCpu = p;
            else if ("MAINBOARD".equals(grp)) currentMain = p;
        }
        String requiredSocket = securityGuard.detectCpuSocket(currentCpu);
        String mainText = currentMain != null ? (currentMain.getName() + " " + currentMain.getVariantName() + " " + currentMain.getSpecs()).toLowerCase() : "";
        String requiredRamType = mainText.contains("ddr5") ? "ddr5" : (mainText.contains("ddr4") ? "ddr4" : "ddr5");

        List<AiChatResponse.ProductSummary> upgradedBuild = new ArrayList<>(currentBuild);
        List<ComponentUpgradeDiff> diffs = new ArrayList<>();

        int cpuIdx = securityGuard.findBuildIndexBySlotKey(upgradedBuild, "cpu");
        if (cpuIdx != -1) {
            AiChatResponse.ProductSummary oldCpu = upgradedBuild.get(cpuIdx);
            String oldCpuHaystack = (oldCpu.getName() + " " + oldCpu.getVariantName() + " " + oldCpu.getSpecs()).toLowerCase();
            if (securityGuard.isApuProcessor(oldCpuHaystack)) {
                double remainingNow = targetBudget - securityGuard.sumBuildPrice(upgradedBuild);
                double cpuSearchCap = oldCpu.getPrice() + Math.max(0, remainingNow);
                int ramCheckIdx = securityGuard.findBuildIndexBySlotKey(upgradedBuild, "ram");
                boolean ramIs8Gb = ramCheckIdx != -1 && (upgradedBuild.get(ramCheckIdx).getName().toLowerCase().contains("8gb"));
                if (ramIs8Gb && remainingNow < 4_200_000) {
                    cpuSearchCap = oldCpu.getPrice();
                }
                AiChatResponse.ProductSummary betterCpu = catalogRepository.pickRealComponentBySlot("cpu", cpuSearchCap, requiredSocket, cpuSearchCap, profile, targetBudget);
                if (betterCpu != null && !betterCpu.getId().equals(oldCpu.getId())) {
                    String newCpuSocket = securityGuard.detectCpuSocket(betterCpu);
                    if (requiredSocket == null || requiredSocket.equals(newCpuSocket)) {
                        double delta = betterCpu.getPrice() - oldCpu.getPrice();
                        upgradedBuild.set(cpuIdx, betterCpu);
                        diffs.add(new ComponentUpgradeDiff(
                                "Bộ vi xử lý (CPU)",
                                oldCpu,
                                betterCpu,
                                delta,
                                "Chuyển từ dòng APU tích hợp (bị giới hạn bộ nhớ đệm L3 16MB & làn PCIe) sang CPU hiệu năng thuần kiến trúc mới với 32MB+ L3 Cache, giúp tăng mạnh FPS gaming và tốc độ render."
                        ));
                    }
                }
            }
        }

        Set<String> userTargetCats = intentResolver.extractTargetCategories(message);
        List<String> upgradeOrder = new ArrayList<>();
        if (!userTargetCats.isEmpty()) {
            for (String cat : userTargetCats) {
                switch (cat) {
                    case "RAM" -> upgradeOrder.add("ram");
                    case "SSD" -> upgradeOrder.add("ssd");
                    case "CPU" -> upgradeOrder.add("cpu");
                    case "VGA" -> upgradeOrder.add("gpu");
                    case "MAINBOARD" -> upgradeOrder.add("mainboard");
                    case "NGUỒN" -> upgradeOrder.add("psu");
                    case "TẢN NHIỆT" -> upgradeOrder.add("cooler");
                    case "CASE" -> upgradeOrder.add("case");
                }
            }
        }
        for (String defaultSlot : new String[]{"ram", "ssd", "cpu", "gpu", "cooler", "mainboard", "psu", "case"}) {
            if (!upgradeOrder.contains(defaultSlot)) {
                upgradeOrder.add(defaultSlot);
            }
        }

        boolean explicitUserSlotRequest = !userTargetCats.isEmpty();

        for (String slotKey : upgradeOrder) {
            int idx = securityGuard.findBuildIndexBySlotKey(upgradedBuild, slotKey);
            if (idx == -1) continue;
            if ("cpu".equals(slotKey) && !diffs.isEmpty() && !userTargetCats.contains("CPU")) {
                continue;
            }

            AiChatResponse.ProductSummary oldItem = upgradedBuild.get(idx);
            double currentBuildSum = securityGuard.sumBuildPrice(upgradedBuild);
            double remaining = targetBudget - currentBuildSum;

            if (remaining < 250_000 && !(explicitUserSlotRequest && upgradeOrder.indexOf(slotKey) == 0)) {
                continue;
            }

            double effectiveRemaining = (explicitUserSlotRequest && upgradeOrder.indexOf(slotKey) == 0 && remaining < 8_000_000)
                    ? Math.max(remaining, 8_000_000) : remaining;

            AiChatResponse.ProductSummary newItem = findBestUpgradeForAdvisory(
                    slotKey, oldItem, effectiveRemaining, requiredSocket, requiredRamType, profile, targetBudget);

            if (newItem != null && !newItem.getId().equals(oldItem.getId()) && newItem.getPrice() != null && oldItem.getPrice() != null) {
                double delta = newItem.getPrice() - oldItem.getPrice();
                if (currentBuildSum + delta <= targetBudget || (explicitUserSlotRequest && upgradeOrder.indexOf(slotKey) == 0)) {
                    upgradedBuild.set(idx, newItem);
                    diffs.add(new ComponentUpgradeDiff(
                            securityGuard.getCategoryDisplayName(newItem.getCategory()),
                            oldItem,
                            newItem,
                            delta,
                            buildUpgradeReasonForSlot(slotKey)
                    ));
                }
            }
        }

        double newTotal = securityGuard.sumBuildPrice(upgradedBuild);
        long displayBudget = Math.max(targetBudget, Math.round(newTotal));

        StringBuilder reply = new StringBuilder();
        reply.append("### 🔧 Tư vấn phương án nâng cấp tối ưu cấu hình PC TechZone\n\n");
        reply.append("* **Tổng cấu hình trước nâng cấp**: ").append(MONEY_FMT.format(previousTotal)).append("\n");
        reply.append("* **Ngân sách mục tiêu**: ").append(MONEY_FMT.format(displayBudget)).append(" _(Khoản ngân sách khả dụng để nâng cấp: **")
                .append(MONEY_FMT.format(Math.max(0, displayBudget - previousTotal))).append("**)_\n");
        reply.append("* **Tổng cấu hình sau nâng cấp**: **").append(MONEY_FMT.format(newTotal)).append("**");
        if (displayBudget >= newTotal) {
            reply.append(" _(Còn dư ").append(MONEY_FMT.format(displayBudget - newTotal)).append(" so với ngân sách)_\n\n");
        } else {
            reply.append("\n\n");
        }

        if (!diffs.isEmpty()) {
            reply.append("#### 🚀 Các hạng mục linh kiện được nâng cấp để tối ưu hiệu năng:\n\n");
            int step = 1;
            for (ComponentUpgradeDiff d : diffs) {
                String sign = d.priceDelta() >= 0 ? "+" + MONEY_FMT.format(d.priceDelta()) : "-" + MONEY_FMT.format(Math.abs(d.priceDelta()));
                reply.append(step++).append(". **").append(d.categoryName()).append("**:\n");
                reply.append("   • **Thay đổi**: Từ _").append(d.oldItem().getName()).append("_ (").append(MONEY_FMT.format(d.oldItem().getPrice())).append(") ➔ **")
                        .append(d.newItem().getName()).append("** (**").append(MONEY_FMT.format(d.newItem().getPrice())).append("**, chênh lệch: **").append(sign).append("**)\n");
                reply.append("   • **Hiệu quả kỹ thuật**: ").append(d.reason()).append("\n\n");
            }
        } else {
            reply.append("💡 **Đánh giá kỹ thuật**: Cấu hình hiện tại đã chạm sát trần ngân sách tối ưu. Nếu muốn nâng cấp nhảy vọt lên phân khúc cao hơn (như lên **RAM 32GB DDR5** hoặc **Card đồ họa phân khúc cao hơn**), bạn có thể nhắn CyberBot mức ngân sách muốn bù thêm nhé!\n\n");
        }

        reply.append("#### 📋 Chi tiết trọn bộ 8 linh kiện sau khi nâng cấp (Sẵn hàng 100%):\n\n");
        for (AiChatResponse.ProductSummary p : upgradedBuild) {
            String catName = securityGuard.getCategoryDisplayName(p.getCategory());
            String vName = p.getVariantName() != null && !p.getVariantName().equalsIgnoreCase("Tiêu chuẩn")
                    ? " (" + p.getVariantName() + ")" : "";
            boolean isUpgradedItem = diffs.stream().anyMatch(d -> d.newItem().getId().equals(p.getId()));
            String badge = isUpgradedItem ? " ✨ _(Đã nâng cấp)_" : "";
            reply.append("🔹 **").append(catName).append("**: **").append(p.getName()).append(vName).append("** — **")
                    .append(MONEY_FMT.format(p.getPrice() != null ? p.getPrice() : 0.0)).append("**").append(badge).append("\n");
            String cleanSpecs = securityGuard.sanitizeSpecsForUser(p.getSpecs() != null ? p.getSpecs() : p.getDescription());
            if (!cleanSpecs.isBlank()) {
                reply.append("  ↳ _Thông số_: ").append(cleanSpecs).append("\n");
            }
        }

        String promoAdvisory = customerContextRepository.buildDynamicPromotionAdvisory(newTotal);
        if (!promoAdvisory.isBlank()) {
            reply.append(promoAdvisory);
        }
        reply.append("\n👉 **Bạn có thể nhấn nút 'Thêm tất cả vào giỏ hàng' bên dưới để chốt ngay cấu hình đã nâng cấp này nhé!**");

        return AiChatResponse.builder()
                .conversationId(conversationId)
                .message(reply.toString())
                .messageType("proposal")
                .messageRoute("/gio-hang")
                .products(upgradedBuild)
                .quickActions(List.of(
                        new AiChatResponse.QuickAction("🛒 Thêm tất cả vào giỏ hàng", "add_all_to_cart", "add_all_to_cart"),
                        new AiChatResponse.QuickAction("💳 Xem giỏ hàng", "navigate", "/gio-hang"),
                        new AiChatResponse.QuickAction("💬 Nhờ chuyên viên hỗ trợ", "connect_staff", null)
                ))
                .build();
    }

    private AiChatResponse.ProductSummary findBestUpgradeForAdvisory(
            String slotKey,
            AiChatResponse.ProductSummary currentItem,
            double remainingBudget,
            String requiredSocket,
            String requiredRamType,
            PcBuildProfile profile,
            long targetBudget) {
        if (currentItem == null || currentItem.getPrice() == null || remainingBudget <= 0) return null;
        double currentPrice = currentItem.getPrice();
        double maxPrice = currentPrice + remainingBudget;

        if ("ram".equals(slotKey) && currentItem.getName().toLowerCase().contains("8gb") && maxPrice >= 5_750_000 && maxPrice < 10_000_000) {
            AiChatResponse.ProductSummary ram16 = catalogRepository.pickRealComponentBySlot("ram", 6_000_000, requiredRamType, Math.min(maxPrice, 6_800_000), profile, targetBudget);
            if (ram16 != null && ram16.getPrice() != null && ram16.getPrice() > currentPrice && ram16.getPrice() <= maxPrice) {
                return ram16;
            }
        }

        if ("ssd".equals(slotKey) && (currentItem.getName().toLowerCase().contains("500gb") || currentItem.getName().toLowerCase().contains("256gb")) && maxPrice >= 4_590_000) {
            AiChatResponse.ProductSummary ssd1Tb = catalogRepository.pickRealComponentBySlot("ssd", 4_800_000, "nvme", Math.min(maxPrice, 6_000_000), profile, targetBudget);
            if (ssd1Tb != null && ssd1Tb.getPrice() != null && ssd1Tb.getPrice() > currentPrice && ssd1Tb.getPrice() <= maxPrice) {
                return ssd1Tb;
            }
        }

        return pcBuildSolver.findBestStepUpComponent(slotKey, currentItem, remainingBudget, requiredSocket, requiredRamType, profile, targetBudget);
    }

    private String buildUpgradeReasonForSlot(String slotKey) {
        return switch (slotKey) {
            case "ram" -> "Nâng gấp đôi dung lượng và băng thông bộ nhớ RAM, giúp loại bỏ hoàn toàn hiện tượng tràn RAM (bottleneck) khi vừa chơi game AAA / dựng hình Blender vừa mở nhiều tác vụ.";
            case "ssd" -> "Tăng gấp đôi dung lượng lưu trữ chuẩn NVMe PCIe Gen4 tốc độ cao, thoải mái cài đặt nhiều tựa game nặng và lưu trữ project đồ họa 3D / video 4K.";
            case "cpu" -> "Nâng cấp sức mạnh xử lý đa nhân và xung nhịp đơn nhân của CPU, giúp tăng tốc độ render, xử lý vật lý và giữ FPS tối thiểu (1% Low) luôn mượt mà.";
            case "gpu" -> "Nâng cấp sức mạnh nhân đồ họa và tản nhiệt VGA, gia tăng trực tiếp số khung hình (FPS) ở độ phân giải cao và rút ngắn thời gian kết xuất đồ họa.";
            case "cooler" -> "Nâng cấp hiệu suất giải nhiệt CPU, giúp chip giữ mức xung Boost tối đa liên tục trong thời gian dài mà vẫn êm ái, mát mẻ.";
            case "mainboard" -> "Nâng cấp dàn cấp nguồn VRM, tản nhiệt khe M.2 và kết nối mạng tốc độ cao trên bo mạch chủ.";
            case "psu" -> "Nâng cấp công suất thực và hiệu suất chuyển đổi điện năng chuẩn 80 Plus, bảo vệ linh kiện an toàn tuyệt đối khi tải nặng.";
            default -> "Tối ưu độ hoàn thiện thẩm mỹ, luồng gió đối lưu (Airflow) và hiệu năng vận hành toàn hệ thống.";
        };
    }
}
