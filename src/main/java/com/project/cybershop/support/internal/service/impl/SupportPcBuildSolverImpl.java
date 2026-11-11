package com.project.cybershop.support.internal.service.impl;

import com.project.cybershop.support.api.dto.request.AiChatRequest;
import com.project.cybershop.support.api.dto.response.AiChatResponse;
import com.project.cybershop.support.internal.model.PcBuildProfile;
import com.project.cybershop.support.internal.repository.SupportCatalogLookupRepository;
import com.project.cybershop.support.internal.repository.SupportCustomerContextRepository;
import com.project.cybershop.support.internal.service.SupportPcBuildSolver;
import com.project.cybershop.support.internal.support.SupportLlmClient;
import com.project.cybershop.support.internal.support.SupportSecurityGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportPcBuildSolverImpl implements SupportPcBuildSolver {

    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###đ");

    private final SupportCatalogLookupRepository catalogRepository;
    private final SupportCustomerContextRepository customerContextRepository;
    private final SupportSecurityGuard securityGuard;
    private final SupportLlmClient llmClient;

    @Override
    public PcBuildProfile parsePcBuildProfile(String message, List<AiChatRequest.HistoryItem> history, long budgetVal) {
        String msgLower = message != null ? message.toLowerCase().trim() : "";
        StringBuilder historyUserContext = new StringBuilder();
        if (history != null) {
            for (int i = history.size() - 1; i >= Math.max(0, history.size() - 6); i--) {
                AiChatRequest.HistoryItem h = history.get(i);
                if ("user".equalsIgnoreCase(h.getSender()) && h.getText() != null) {
                    historyUserContext.append(" ").append(h.getText().toLowerCase());
                }
            }
        }
        String combined = (msgLower + " " + historyUserContext).trim();

        String profileType = null;
        if (msgLower.matches(".*\\b(blender|3d|render|đồ họa|đồ hoạ|dựng phim|premiere|after effects|maya|3dsmax|lumion|vray|autocad|revit|sketchup|ai|deep learning|lập trình|code|máy ảo|workstation|máy trạm|đa nhiệm)\\b.*")) {
            profileType = "WORKSTATION_3D";
        } else if (msgLower.matches(".*\\b(chơi game|gaming|game|fps|esport|valorant|cs2|pubg|lol|wukong|cyberpunk|gta|aaa|stream)\\b.*")) {
            profileType = "GAMING";
        } else if (msgLower.matches(".*\\b(văn phòng|office|word|excel|kế toán|học tập|nhẹ nhàng|cơ bản)\\b.*")) {
            profileType = "OFFICE";
        }

        if (profileType == null) {
            String histStr = historyUserContext.toString();
            if (histStr.matches(".*\\b(blender|3d|render|đồ họa|đồ hoạ|dựng phim|premiere|after effects|maya|3dsmax|lumion|vray|autocad|revit|sketchup|ai|deep learning|lập trình|code|máy ảo|workstation|máy trạm|đa nhiệm)\\b.*")) {
                profileType = "WORKSTATION_3D";
            } else if (histStr.matches(".*\\b(chơi game|gaming|game|fps|esport|valorant|cs2|pubg|lol|wukong|cyberpunk|gta|aaa|stream)\\b.*")) {
                profileType = "GAMING";
            } else if (histStr.matches(".*\\b(văn phòng|office|word|excel|kế toán|học tập|nhẹ nhàng|cơ bản)\\b.*")) {
                profileType = "OFFICE";
            } else {
                profileType = budgetVal < 14_000_000L ? "OFFICE" : "GAMING";
            }
        }

        boolean preferAesthetic = combined.matches(".*(đẹp|nhiều đèn|rgb|argb|led|lung linh|bể cá|show-case|showcase|trắng|white).*");
        boolean preferWhite = combined.matches(".*(trắng|white|bạch tuyết|full trắng).*");

        String preferredCpuBrand = null;
        if (msgLower.matches(".*\\b(intel|core i|core ultra|i5|i7|i9)\\b.*")) {
            preferredCpuBrand = "intel";
        } else if (msgLower.matches(".*\\b(amd|ryzen)\\b.*")) {
            preferredCpuBrand = "amd";
        } else if ("WORKSTATION_3D".equals(profileType) && budgetVal >= 30_000_000L && budgetVal <= 48_000_000L) {
            preferredCpuBrand = "intel";
        }

        String preferredGpuBrand = null;
        if (msgLower.matches(".*\\b(radeon|rx)\\b.*")) {
            preferredGpuBrand = "amd";
        } else if (msgLower.matches(".*\\b(nvidia|rtx|geforce|gtx)\\b.*") || "WORKSTATION_3D".equals(profileType)) {
            preferredGpuBrand = "nvidia";
        }

        Integer minRamGb;
        if (combined.contains("64gb")) minRamGb = 64;
        else if (combined.contains("48gb")) minRamGb = 48;
        else if (combined.contains("32gb")) minRamGb = 32;
        else if (combined.contains("16gb")) minRamGb = 16;
        else {
            if ("WORKSTATION_3D".equals(profileType)) {
                minRamGb = budgetVal >= 58_000_000L ? 32 : (budgetVal >= 20_000_000L ? 16 : 8);
            } else {
                minRamGb = budgetVal >= 68_000_000L ? 32 : (budgetVal >= 24_000_000L ? 16 : 8);
            }
        }

        Integer minSsdGb = null;
        if (combined.contains("4tb")) minSsdGb = 4000;
        else if (combined.contains("2tb")) minSsdGb = 2000;
        else if (combined.contains("1tb")) minSsdGb = 1000;
        else {
            if ("WORKSTATION_3D".equals(profileType) && budgetVal >= 30_000_000L) {
                minSsdGb = 1000;
            } else if (budgetVal >= 45_000_000L) {
                minSsdGb = 1000;
            }
        }

        return new PcBuildProfile(profileType, preferAesthetic, preferWhite, preferredCpuBrand, preferredGpuBrand, minRamGb, minSsdGb);
    }

    @Override
    public List<AiChatResponse.ProductSummary> buildRealPcComponentsForBudget(long budgetVal) {
        return buildRealPcComponentsForBudget(budgetVal, null, null);
    }

    @Override
    public List<AiChatResponse.ProductSummary> buildRealPcComponentsForBudget(
            long budgetVal,
            String userMessage,
            List<AiChatRequest.HistoryItem> history) {
        PcBuildProfile profile = parsePcBuildProfile(userMessage, history, budgetVal);
        List<AiChatResponse.ProductSummary> build = new ArrayList<>();

        double cpuRatio = "WORKSTATION_3D".equals(profile.profileType()) ? 0.215 : ("GAMING".equals(profile.profileType()) ? 0.235 : 0.21);
        double mainRatio = "WORKSTATION_3D".equals(profile.profileType()) ? 0.135 : 0.125;
        double ramRatio = "WORKSTATION_3D".equals(profile.profileType()) ? 0.165 : (budgetVal >= 25_000_000L ? 0.145 : 0.11);
        double vgaRatio = "WORKSTATION_3D".equals(profile.profileType()) ? 0.305 : ("GAMING".equals(profile.profileType()) ? 0.345 : 0.32);
        double ssdRatio = "WORKSTATION_3D".equals(profile.profileType()) ? 0.105 : 0.075;
        double psuRatio = 0.05;
        double caseRatio = profile.preferAesthetic() ? 0.042 : 0.033;
        double coolerRatio = profile.preferAesthetic() && budgetVal >= 50_000_000L ? 0.05 : 0.025;

        AiChatResponse.ProductSummary cpu = catalogRepository.pickRealComponentBySlot("cpu", budgetVal * cpuRatio, null, null, profile, budgetVal);
        if (cpu == null) cpu = catalogRepository.pickRealComponentBySlot("vi xử lý", budgetVal * cpuRatio, null, null, profile, budgetVal);

        String requiredSocket = securityGuard.detectCpuSocket(cpu);

        AiChatResponse.ProductSummary main = catalogRepository.pickRealComponentBySlot("mainboard", budgetVal * mainRatio, requiredSocket, null, profile, budgetVal);
        if (main == null) main = catalogRepository.pickRealComponentBySlot("bo mạch", budgetVal * mainRatio, requiredSocket, null, profile, budgetVal);

        String mainText = main != null ? (main.getName() + " " + main.getVariantName() + " " + main.getSpecs() + " " + main.getDescription()).toLowerCase() : "";
        String requiredRamType = mainText.contains("ddr5") ? "ddr5" : (mainText.contains("ddr4") ? "ddr4" : ("am5".equals(requiredSocket) || "lga1851".equals(requiredSocket) || budgetVal >= 22_000_000L ? "ddr5" : "ddr4"));

        AiChatResponse.ProductSummary ram = catalogRepository.pickRealComponentBySlot("ram", budgetVal * ramRatio, requiredRamType, null, profile, budgetVal);

        AiChatResponse.ProductSummary vga = catalogRepository.pickRealComponentBySlot("gpu", budgetVal * vgaRatio, null, null, profile, budgetVal);
        if (vga == null) vga = catalogRepository.pickRealComponentBySlot("card", budgetVal * vgaRatio, null, null, profile, budgetVal);

        AiChatResponse.ProductSummary ssd = catalogRepository.pickRealComponentBySlot("ssd", budgetVal * ssdRatio, "nvme", null, profile, budgetVal);
        if (ssd == null) ssd = catalogRepository.pickRealComponentBySlot("ổ cứng", budgetVal * ssdRatio, null, null, profile, budgetVal);

        AiChatResponse.ProductSummary psu = catalogRepository.pickRealComponentBySlot("psu", budgetVal * psuRatio, null, null, profile, budgetVal);
        if (psu == null) psu = catalogRepository.pickRealComponentBySlot("nguồn", budgetVal * psuRatio, null, null, profile, budgetVal);

        AiChatResponse.ProductSummary pcCase = catalogRepository.pickRealComponentBySlot("case", budgetVal * caseRatio, null, null, profile, budgetVal);
        if (pcCase == null) pcCase = catalogRepository.pickRealComponentBySlot("vỏ", budgetVal * caseRatio, null, null, profile, budgetVal);

        AiChatResponse.ProductSummary cooler = catalogRepository.pickRealComponentBySlot("cooler", budgetVal * coolerRatio, requiredSocket, null, profile, budgetVal);
        if (cooler == null) cooler = catalogRepository.pickRealComponentBySlot("tản", budgetVal * coolerRatio, requiredSocket, null, profile, budgetVal);

        List<AiChatResponse.ProductSummary> rawList = new ArrayList<>(Arrays.asList(cpu, main, ram, vga, ssd, psu, pcCase, cooler));
        for (AiChatResponse.ProductSummary item : rawList) {
            if (item != null && item.getId() != null) {
                build.add(item);
            }
        }

        String[] downSlotKeys = {"cooler", "case", "psu", "mainboard", "ssd", "cpu", "ram", "gpu"};
        String[] downFallbacks = {"tản", "vỏ", "nguồn", "bo mạch", "ổ cứng", "vi xử lý", "ram", "card"};
        String[] downCompat = {requiredSocket, null, null, requiredSocket, "nvme", requiredSocket, requiredRamType, null};

        for (int pass = 0; pass < 4; pass++) {
            double currentTotal = securityGuard.sumBuildPrice(build);
            if (currentTotal <= budgetVal) {
                break;
            }

            for (int idx = 0; idx < downSlotKeys.length; idx++) {
                currentTotal = securityGuard.sumBuildPrice(build);
                double excess = currentTotal - budgetVal;
                if (excess <= 0) {
                    break;
                }

                int buildIdx = securityGuard.findBuildIndexBySlotKey(build, downSlotKeys[idx]);
                if (buildIdx == -1) continue;

                AiChatResponse.ProductSummary currentItem = build.get(buildIdx);
                double currentPrice = currentItem.getPrice() != null ? currentItem.getPrice() : 0.0;
                double maxAllowedPrice = Math.max(currentPrice - excess, currentPrice * 0.50);
                if (maxAllowedPrice >= currentPrice) {
                    maxAllowedPrice = currentPrice - 10_000;
                }

                AiChatResponse.ProductSummary cheaper = catalogRepository.pickRealComponentBySlot(downSlotKeys[idx], maxAllowedPrice, downCompat[idx], maxAllowedPrice, profile, budgetVal);
                if (cheaper == null) {
                    cheaper = catalogRepository.pickRealComponentBySlot(downFallbacks[idx], maxAllowedPrice, downCompat[idx], maxAllowedPrice, profile, budgetVal);
                }
                if (cheaper != null && cheaper.getPrice() != null && cheaper.getPrice() < currentPrice) {
                    if ("cpu".equals(downSlotKeys[idx])) {
                        String newSock = securityGuard.detectCpuSocket(cheaper);
                        if (requiredSocket != null && newSock != null && !requiredSocket.equals(newSock)) {
                            continue;
                        }
                    }
                    build.set(buildIdx, cheaper);
                }
            }
        }

        String[] upSlotKeys = "WORKSTATION_3D".equals(profile.profileType())
                ? new String[]{"ram", "ssd", "cpu", "gpu", "mainboard", "cooler", "psu", "case"}
                : new String[]{"gpu", "ram", "cpu", "ssd", "mainboard", "cooler", "psu", "case"};

        for (int pass = 0; pass < 3; pass++) {
            double currentTotal = securityGuard.sumBuildPrice(build);
            double remaining = budgetVal - currentTotal;
            if (remaining < 350_000) {
                break;
            }
            boolean anyUpgraded = false;
            for (String slotKey : upSlotKeys) {
                currentTotal = securityGuard.sumBuildPrice(build);
                remaining = budgetVal - currentTotal;
                if (remaining < 350_000) {
                    break;
                }
                int buildIdx = securityGuard.findBuildIndexBySlotKey(build, slotKey);
                if (buildIdx == -1) continue;

                AiChatResponse.ProductSummary currentItem = build.get(buildIdx);
                AiChatResponse.ProductSummary upgraded = findBestStepUpComponent(
                        slotKey, currentItem, remaining, requiredSocket, requiredRamType, profile, budgetVal);
                if (upgraded != null && upgraded.getPrice() != null && currentItem.getPrice() != null
                        && upgraded.getPrice() > currentItem.getPrice()
                        && (currentTotal - currentItem.getPrice() + upgraded.getPrice()) <= budgetVal) {
                    build.set(buildIdx, upgraded);
                    anyUpgraded = true;
                }
            }
            if (!anyUpgraded) {
                break;
            }
        }

        return build;
    }

    @Override
    public AiChatResponse.ProductSummary findBestStepUpComponent(
            String slotKey,
            AiChatResponse.ProductSummary currentItem,
            double remainingBudget,
            String requiredSocket,
            String requiredRamType,
            PcBuildProfile profile,
            long totalBudgetVal) {
        if (currentItem == null || currentItem.getPrice() == null || remainingBudget <= 0) return null;
        double currentPrice = currentItem.getPrice();
        double maxUpgradePrice = currentPrice + remainingBudget;

        if ("cooler".equals(slotKey) && maxUpgradePrice > Math.max(1_500_000, totalBudgetVal * 0.06)) {
            maxUpgradePrice = Math.max(currentPrice, Math.min(maxUpgradePrice, Math.max(1_500_000, totalBudgetVal * 0.06)));
        } else if ("case".equals(slotKey) && maxUpgradePrice > Math.max(2_200_000, totalBudgetVal * 0.055)) {
            maxUpgradePrice = Math.max(currentPrice, Math.min(maxUpgradePrice, Math.max(2_200_000, totalBudgetVal * 0.055)));
        } else if ("psu".equals(slotKey) && maxUpgradePrice > Math.max(2_500_000, totalBudgetVal * 0.065)) {
            maxUpgradePrice = Math.max(currentPrice, Math.min(maxUpgradePrice, Math.max(2_500_000, totalBudgetVal * 0.065)));
        } else if ("mainboard".equals(slotKey) && maxUpgradePrice > Math.max(6_500_000, totalBudgetVal * 0.16)) {
            maxUpgradePrice = Math.max(currentPrice, Math.min(maxUpgradePrice, Math.max(6_500_000, totalBudgetVal * 0.16)));
        }
        if (maxUpgradePrice <= currentPrice) return null;

        String compatToken = switch (slotKey) {
            case "cpu", "mainboard", "cooler" -> requiredSocket;
            case "ram" -> requiredRamType;
            case "ssd" -> "nvme";
            default -> null;
        };

        AiChatResponse.ProductSummary candidate = catalogRepository.pickRealComponentBySlot(
                slotKey, maxUpgradePrice, compatToken, maxUpgradePrice, profile, totalBudgetVal);
        if (candidate == null || candidate.getPrice() == null || candidate.getPrice() <= currentPrice || candidate.getPrice() > (currentPrice + remainingBudget)) {
            return null;
        }

        if ("ram".equals(slotKey)) {
            int oldCap = securityGuard.extractCapacityGb(currentItem);
            int newCap = securityGuard.extractCapacityGb(candidate);
            if (oldCap > 0 && newCap <= oldCap && (profile == null || !profile.preferAesthetic())) {
                return null;
            }
        }

        String candHaystack = (candidate.getName() + " " + candidate.getVariantName() + " " + candidate.getSpecs()).toLowerCase();
        if ("cpu".equals(slotKey)) {
            if (securityGuard.isApuProcessor(candHaystack)) return null;
            String candSocket = securityGuard.detectCpuSocket(candidate);
            if (requiredSocket != null && candSocket != null && !requiredSocket.equals(candSocket)) {
                return null;
            }
        }
        if ("gpu".equals(slotKey) && profile != null && "nvidia".equals(profile.preferredGpuBrand())) {
            if (!candHaystack.contains("rtx") && !candHaystack.contains("geforce")) {
                return null;
            }
        }
        return candidate;
    }

    @Override
    public AiChatResponse formatSmartPcBuildAdvisory(String message, BigDecimal budget, String conversationId) {
        return formatSmartPcBuildAdvisory(message, budget, null, conversationId);
    }

    @Override
    public AiChatResponse formatSmartPcBuildAdvisory(
            String message,
            BigDecimal budget,
            List<AiChatRequest.HistoryItem> history,
            String conversationId) {
        long budgetVal = budget != null ? budget.longValue() : 25_000_000L;
        PcBuildProfile profile = parsePcBuildProfile(message, history, budgetVal);
        List<AiChatResponse.ProductSummary> productCards = buildRealPcComponentsForBudget(budgetVal, message, history);

        double totalVal = securityGuard.sumBuildPrice(productCards);

        StringBuilder reply = new StringBuilder();
        String buildName;
        String buildPurpose;
        String technicalEvaluation;

        if ("WORKSTATION_3D".equals(profile.profileType())) {
            if (budgetVal < 25_000_000L) {
                buildName = "PC Đồ Họa 2D/3D & Dựng Phim Đa Nhiệm TechZone";
            } else if (budgetVal < 48_000_000L) {
                buildName = "PC Đồ Họa 3D, Render Blender & Workstation Chuyên Nghiệp TechZone";
            } else {
                buildName = "Siêu Máy Trạm Workstation 3D Render, Blender & AI Hi-End TechZone";
            }
            buildPurpose = "Tối ưu chuyên biệt cho dựng hình 3D (Blender, Maya, 3dsMax), Render CUDA/OptiX, biên tập video 4K và đa nhiệm nặng.";
            technicalEvaluation = "Cấu hình ưu tiên **CPU đa nhân đa luồng** xử lý tính toán vật lý/modeling, kết hợp **Card đồ họa NVIDIA GeForce RTX** (hỗ trợ tăng tốc phần cứng nhân CUDA & OptiX trong Blender Cycles/Eevee), **RAM băng thông cao** chống tràn bộ nhớ và **SSD NVMe Gen4** tốc độ đọc/ghi cao để load file project 3D mượt mà.";
        } else if ("OFFICE".equals(profile.profileType())) {
            buildName = "PC Văn Phòng, Học Tập & Làm Việc Đa Nhiệm Bền Bỉ TechZone";
            buildPurpose = "Vận hành êm ái, mát mẻ, tiết kiệm điện năng và xử lý mượt mà mọi tác vụ văn phòng, kế toán, học tập và giải trí nhẹ.";
            technicalEvaluation = "Tất cả linh kiện được chọn trực tiếp từ kho TechZone đang còn hàng, đảm bảo tương thích 100% chuẩn Socket và chuẩn RAM, khởi động Windows tức thì với ổ cứng SSD NVMe.";
        } else {
            if (budgetVal < 18_000_000L) {
                buildName = "PC Gaming Esport & Chiến Game Phổ Thông Mượt Mà TechZone";
            } else if (budgetVal < 33_000_000L) {
                buildName = "PC Gaming Chiến Mượt AAA & Esport FPS Cao TechZone";
            } else if (budgetVal < 58_000_000L) {
                buildName = "PC Gaming Hiệu Năng Cao Chiến Mượt AAA 2K/4K Max Setting TechZone";
            } else {
                buildName = "Siêu PC Gaming Flagship 4K Ultra & Ray Tracing Đỉnh Cao TechZone";
            }
            buildPurpose = "Tối ưu hóa hiệu năng chơi game (FPS cao, frametime ổn định, nhiệt độ mát mẻ) kết hợp CPU chuyên game và Card đồ họa rời mạnh mẽ nhất trong tầm giá.";
            technicalEvaluation = "Cấu hình loại bỏ hoàn toàn nghẽn cổ chai (bottleneck) bằng việc kết hợp **CPU chuyên Gaming (xung nhịp cao / bộ nhớ đệm L3 lớn, không dùng APU cắt giảm làn PCIe)** cùng **Card đồ họa rời hiệu năng cao**, **RAM chuẩn mới** và bộ nguồn công suất thực chuẩn 80 Plus an toàn tuyệt đối khi chiến game liên tục.";
        }

        if (profile.preferAesthetic()) {
            buildName += profile.preferWhite() ? " (Phiên bản Tone Trắng ARGB Show-Case)" : " (Phiên bản Show-Case ARGB Thẩm Mỹ Cao)";
        }

        reply.append("### 🎯 Cấu hình đề xuất: **").append(buildName).append("**\n\n");
        reply.append("* **Mục đích**: ").append(buildPurpose).append("\n");
        reply.append("* **Ngân sách dự kiến**: ").append(MONEY_FMT.format(budgetVal)).append("\n");
        reply.append("* **Tổng giá trị thực tế**: **").append(MONEY_FMT.format(totalVal)).append("**");
        if (budgetVal >= totalVal) {
            reply.append(" _(Tiết kiệm ").append(MONEY_FMT.format(budgetVal - totalVal)).append(" so với dự toán)_\n\n");
        } else {
            reply.append("\n* ⚠️ _Lưu ý kỹ thuật: Để lắp ráp trọn bộ đầy đủ 8 linh kiện PC mới 100% chính hãng (kèm Card đồ họa rời) đang sẵn hàng tại kho TechZone, mức chi phí tối thiểu bắt đầu từ **")
                    .append(MONEY_FMT.format(totalVal)).append("**. Dưới đây là cấu hình chuẩn tiết kiệm nhất tại kho để bạn tham khảo:_\n\n");
        }

        reply.append("#### 📋 Chi tiết trọn bộ linh kiện sẵn hàng (Tương thích 100% phần cứng):\n\n");
        StringBuilder lockedFacts = new StringBuilder();
        lockedFacts.append("Cấu hình: ").append(buildName)
                .append(" | Ngân sách: ").append(MONEY_FMT.format(budgetVal))
                .append(" | Tổng giá thực tế: ").append(MONEY_FMT.format(totalVal)).append("\n");
        for (AiChatResponse.ProductSummary p : productCards) {
            String catName = securityGuard.getCategoryDisplayName(p.getCategory());
            String vName = p.getVariantName() != null && !p.getVariantName().equalsIgnoreCase("Tiêu chuẩn")
                    ? " (" + p.getVariantName() + ")" : "";
            String priceStr = MONEY_FMT.format(p.getPrice() != null ? p.getPrice() : 0.0);
            reply.append("🔹 **").append(catName).append("**: **").append(p.getName()).append(vName).append("** — **")
                    .append(priceStr).append("**\n");
            String cleanSpecs = securityGuard.sanitizeSpecsForUser(p.getSpecs() != null ? p.getSpecs() : p.getDescription());
            if (!cleanSpecs.isBlank()) {
                reply.append("  ↳ _Thông số_: ").append(cleanSpecs).append("\n");
            }
            lockedFacts.append("- ").append(catName).append(": ").append(p.getName()).append(vName)
                    .append(" (").append(priceStr).append(") [").append(cleanSpecs).append("]\n");
        }
        reply.append("\n💡 **Đánh giá tương thích từ Kỹ sư phần cứng TechZone:**\n");
        String groundedAiCommentary = llmClient.generateGroundedCommentary(
                "PC_BUILD_ADVISORY", message, lockedFacts.toString(), null);
        if (groundedAiCommentary != null && !groundedAiCommentary.isBlank()) {
            reply.append(groundedAiCommentary).append("\n");
            reply.append("• _Kiểm định tương thích chuẩn TechZone_: ").append(technicalEvaluation).append("\n\n");
        } else {
            reply.append(technicalEvaluation).append("\n\n");
        }
        reply.append("🎁 **Đặc quyền khi lắp máy trọn bộ tại TechZone**:\n");
        reply.append("• Miễn phí 100% công lắp ráp và đi dây thẩm mỹ chuẩn show-case.\n");
        reply.append("• Kiểm tra Stress-Test (FurMark + Prime95) nhiệt độ ổn định trước khi giao.\n");
        reply.append("• Kích hoạt bảo hành điện tử chính hãng từng Serial linh kiện ngay khi nhận máy.\n");
        String promoAdvisory = customerContextRepository.buildDynamicPromotionAdvisory(totalVal);
        if (!promoAdvisory.isBlank()) {
            reply.append(promoAdvisory);
        }
        reply.append("\n👉 **Bạn có thể nhấn nút 'Thêm tất cả vào giỏ hàng' bên dưới hoặc nhắn CyberBot nếu muốn nâng cấp/thay đổi bất kỳ linh kiện nào nhé!**");

        List<AiChatResponse.QuickAction> actions = new ArrayList<>();
        actions.add(new AiChatResponse.QuickAction("🛒 Thêm tất cả vào giỏ hàng", "add_all_to_cart", "add_all_to_cart"));
        actions.add(new AiChatResponse.QuickAction("💳 Xem giỏ hàng", "navigate", "/gio-hang"));
        actions.add(new AiChatResponse.QuickAction("💬 Nhờ chuyên viên hỗ trợ", "connect_staff", null));

        return AiChatResponse.builder()
                .conversationId(conversationId)
                .message(reply.toString())
                .messageType("proposal")
                .messageRoute("/gio-hang")
                .quickActions(actions)
                .products(productCards)
                .build();
    }
}
