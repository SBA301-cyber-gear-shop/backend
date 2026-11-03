package com.project.cybershop.pcbuild.internal.support;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PeripheralSelector - chọn màn, phím, chuột theo tiền còn lại")
class PeripheralSelectorTest {

    /** Cùng giá và tần số quét với mục 10 của docs/newMetadata.sql. */
    static List<ComponentCandidateDTO> catalog() {
        List<ComponentCandidateDTO> items = new ArrayList<>();
        long[][] monitors = {{201, 2_790_000, 60}, {202, 2_490_000, 100}, {203, 3_890_000, 180}, {204, 6_190_000, 180},
                {205, 6_990_000, 170}, {206, 7_290_000, 180}, {207, 9_490_000, 165}, {208, 13_490_000, 60},
                {209, 18_990_000, 240}, {210, 27_990_000, 240}};
        for (long[] row : monitors) items.add(monitor((int) row[0], row[1], String.valueOf(row[2])));
        long[][] keyboards = {{211, 190_000}, {212, 490_000}, {213, 690_000}, {214, 1_090_000}, {215, 1_290_000},
                {216, 1_990_000}, {217, 2_790_000}, {218, 2_990_000}, {219, 3_290_000}, {220, 4_990_000}};
        for (long[] row : keyboards) items.add(item((int) row[0], "keyboard", row[1]));
        long[][] mice = {{221, 120_000}, {222, 290_000}, {223, 420_000}, {224, 490_000}, {225, 750_000},
                {226, 790_000}, {227, 1_290_000}, {228, 1_490_000}, {229, 2_390_000}, {230, 3_390_000}};
        for (long[] row : mice) items.add(item((int) row[0], "mouse", row[1]));
        return items;
    }

    static ComponentCandidateDTO item(int id, String slug, Long price) {
        return ComponentCandidateDTO.builder().variantId(id).categorySlug(slug)
                .price(price != null ? BigDecimal.valueOf(price) : null).build();
    }

    static ComponentCandidateDTO monitor(int id, long price, String refreshHz) {
        return ComponentCandidateDTO.builder().variantId(id).categorySlug("monitor").price(BigDecimal.valueOf(price))
                .specValuesById(refreshHz != null ? Map.of(PeripheralSelector.REFRESH_RATE_HZ, refreshHz) : Map.of())
                .build();
    }

    private static PeripheralSelector.Selection select(List<ComponentCandidateDTO> items, long budget, long pcTotal, String archetype) {
        return PeripheralSelector.select(items, BigDecimal.valueOf(budget), BigDecimal.valueOf(pcTotal), archetype);
    }

    private static int picked(PeripheralSelector.Selection selection, String role) {
        return selection.picks().get(role).getVariantId();
    }

    private static void assertPrice(BigDecimal actual, long expected) {
        assertThat(actual).isEqualByComparingTo(BigDecimal.valueOf(expected));
    }

    @ParameterizedTest(name = "ngân sách {0}, thùng {1}, {2}: màn {3}, phím {4}, chuột {5}")
    @CsvSource({
            // Mức thấp nhất: đủ đúng 3 món rẻ nhất
            "12000000,  9200000, GENERAL, 202, 211, 221",
            // Ví dụ 30 triệu chơi game
            "30000000, 23000000, GAMING,  203, 213, 224",
            // Máy văn phòng rẻ không bị gợi ý màn đắt dù còn dư tiền (mức 25% giá thùng máy)
            "100000000, 20000000, OFFICE, 201, 213, 224",
            // Dư tiền, không chơi game: màn đắt nhất trong giới hạn
            "100000000, 80000000, GENERAL, 208, 218, 228",
            // Dư tiền, chơi game: bỏ màn 13,49 triệu 60Hz, lấy màn 165Hz
            "100000000, 80000000, GAMING,  207, 218, 228",
            // Ngân sách rất cao: 3 món tốt nhất
            "300000000, 263630000, GAMING, 210, 220, 230",
            "1000000000, 963630000, GENERAL, 210, 220, 230",
    })
    @DisplayName("chọn đúng 3 món theo công thức")
    void picksExpectedItems(long budget, long pcTotal, String archetype, int monitor, int keyboard, int mouse) {
        PeripheralSelector.Selection selection = select(catalog(), budget, pcTotal, archetype);

        assertThat(picked(selection, "monitor")).isEqualTo(monitor);
        assertThat(picked(selection, "keyboard")).isEqualTo(keyboard);
        assertThat(picked(selection, "mouse")).isEqualTo(mouse);
    }

    @ParameterizedTest(name = "ngân sách {0}, thùng {1}")
    @CsvSource({"10800000,8000000", "12000000,9200000", "30000000,23000000", "30000000,24000000", "33000000,30000000",
            "50000000,40000000", "100000000,20000000", "300000000,263630000", "9999999999,9000000000"})
    @DisplayName("tổng phụ kiện không bao giờ vượt tiền còn lại, tổng cả bộ không vượt ngân sách")
    void neverExceedsBudget(long budget, long pcTotal) {
        PeripheralSelector.Selection selection = select(catalog(), budget, pcTotal, "GAMING");

        assertThat(selection.peripheralTotal()).isLessThanOrEqualTo(selection.available());
        assertThat(selection.peripheralTotal()).isLessThanOrEqualTo(selection.spendLimit());
        assertThat(BigDecimal.valueOf(pcTotal).add(selection.peripheralTotal())).isLessThanOrEqualTo(BigDecimal.valueOf(budget));
    }

    @Test
    @DisplayName("công thức tiền: spendLimit = min(còn lại, max(tổng rẻ nhất, 25% thùng máy))")
    void computesSpendLimit() {
        PeripheralSelector.Selection normal = select(catalog(), 30_000_000, 23_000_000, "GAMING");
        assertPrice(normal.available(), 7_000_000);
        assertPrice(normal.spendLimit(), 5_750_000);
        assertPrice(normal.peripheralTotal(), 5_070_000);

        // 25% thùng máy > tiền còn lại: bị chặn bởi tiền còn lại
        assertPrice(select(catalog(), 33_000_000, 30_000_000, "GENERAL").spendLimit(), 3_000_000);
        // 25% thùng máy < tổng rẻ nhất: vẫn đủ mua 3 món
        assertPrice(select(catalog(), 12_000_000, 9_000_000, "GENERAL").spendLimit(), 2_800_000);
    }

    @Test
    @DisplayName("GAMING không đủ tiền cho màn từ 144Hz: vẫn trả màn thường và bật cờ để báo khách")
    void gamingFallsBackWhenHighRefreshIsUnaffordable() {
        PeripheralSelector.Selection selection = select(catalog(), 12_000_000, 9_000_000, "GAMING");

        assertThat(picked(selection, "monitor")).isEqualTo(202);
        assertThat(selection.gamingFallback()).isTrue();
        assertThat(select(catalog(), 30_000_000, 23_000_000, "GAMING").gamingFallback()).isFalse();
        // Không chơi game thì không có khái niệm thiếu màn tần số cao
        assertThat(select(catalog(), 12_000_000, 9_000_000, "OFFICE").gamingFallback()).isFalse();
    }

    @Test
    @DisplayName("B2: chơi game, đủ tiền cho màn từ 144Hz nếu lấy phím chuột rẻ hơn -> ưu tiên màn tần số cao")
    void gamingPrefersHighRefreshWhenCheaperAccessoriesMakeRoom() {
        // Giống ca 20 triệu ở User testing: còn 4,101 triệu, giới hạn chi 3,97 triệu
        List<ComponentCandidateDTO> items = new ArrayList<>(catalog());
        items.add(monitor(299, 3_290_000, "180"));

        PeripheralSelector.Selection selection = select(items, 20_000_000, 15_899_000, "GAMING");

        assertThat(picked(selection, "monitor")).isEqualTo(299);
        assertThat(selection.gamingFallback()).isFalse();
        assertThat(selection.peripheralTotal()).isLessThanOrEqualTo(selection.spendLimit());
        // Không chơi game thì giữ cách chọn cũ
        assertThat(select(items, 20_000_000, 15_899_000, "OFFICE").picks().get("monitor").getVariantId()).isNotEqualTo(299);
    }

    @Test
    @DisplayName("B3: trần = số tiền bộ chọn thật sự chi được, không phải tổng giá đắt nhất; ngân sách lớn không bỏ phí")
    void budgetRangeMaxIsWhatTheSelectorCanSpend() {
        List<ComponentCandidateDTO> items = new ArrayList<>(catalog());
        items.add(item(500, "keyboard", 50_000_000L)); // bàn phím 50 triệu: bộ chọn không bao giờ tự lấy

        PeripheralSelector.BudgetRange range = PeripheralSelector.budgetRange(items);

        assertThat(range.max()).isLessThan(BigDecimal.valueOf(27_990_000L + 50_000_000L + 3_390_000L));
        for (String archetype : new String[]{"GENERAL", "GAMING"}) {
            // Thùng máy rất đắt: tiền còn lại đúng bằng trần -> chi hết, không dư
            PeripheralSelector.Selection selection = PeripheralSelector.select(items,
                    BigDecimal.valueOf(5_000_000_000L).add(range.max()), BigDecimal.valueOf(5_000_000_000L), archetype);
            assertThat(selection.peripheralTotal()).isLessThanOrEqualTo(range.max());
            assertThat(range.max().subtract(selection.peripheralTotal())).isLessThan(BigDecimal.valueOf(100_000));
        }
    }

    @Test
    @DisplayName("archetype không phân biệt hoa thường; null hoặc giá trị lạ không áp luật chơi game")
    void archetypeIsParsedLeniently() {
        assertThat(picked(select(catalog(), 100_000_000, 80_000_000, "gaming"), "monitor")).isEqualTo(207);
        assertThat(picked(select(catalog(), 100_000_000, 80_000_000, null), "monitor")).isEqualTo(208);
        assertThat(picked(select(catalog(), 100_000_000, 80_000_000, "HACK"), "monitor")).isEqualTo(208);
    }

    @Test
    @DisplayName("màn thiếu hoặc sai Refresh_Rate_Hz không được coi là màn tần số cao")
    void malformedRefreshRateIsNotHighRefresh() {
        List<ComponentCandidateDTO> items = new ArrayList<>(catalog().stream()
                .filter(item -> !"monitor".equals(item.getCategorySlug())).toList());
        items.add(monitor(301, 3_000_000, null));
        items.add(monitor(302, 3_100_000, "nhanh"));
        items.add(monitor(303, 2_000_000, "144"));

        PeripheralSelector.Selection selection = select(items, 30_000_000, 23_000_000, "GAMING");

        assertThat(picked(selection, "monitor")).isEqualTo(303);
        assertThat(selection.gamingFallback()).isFalse();
    }

    @Test
    @DisplayName("giá bằng nhau: lấy variantId nhỏ hơn, không phụ thuộc thứ tự dữ liệu")
    void tieBreaksByLowestVariantId() {
        List<ComponentCandidateDTO> items = new ArrayList<>(List.of(monitor(402, 3_000_000, "60"), monitor(401, 3_000_000, "60"),
                item(412, "keyboard", 300_000L), item(411, "keyboard", 300_000L),
                item(422, "mouse", 200_000L), item(421, "mouse", 200_000L)));

        PeripheralSelector.Selection selection = select(items, 30_000_000, 20_000_000, "GENERAL");

        assertThat(List.of(picked(selection, "monitor"), picked(selection, "keyboard"), picked(selection, "mouse")))
                .containsExactly(401, 411, 421);
    }

    @Test
    @DisplayName("món không có giá bị bỏ qua khi chọn và khi tính khoảng ngân sách")
    void ignoresItemsWithoutPrice() {
        List<ComponentCandidateDTO> items = new ArrayList<>(catalog());
        items.add(item(500, "mouse", null));

        assertThat(PeripheralSelector.optionsOf(items, "mouse")).extracting(ComponentCandidateDTO::getVariantId)
                .doesNotContain(500).hasSize(10);
        assertPrice(PeripheralSelector.budgetRange(items).min(), 2_800_000);
    }

    @Test
    @DisplayName("danh sách lựa chọn sắp theo giá tăng dần")
    void optionsAreSortedByPrice() {
        assertThat(PeripheralSelector.optionsOf(catalog(), "monitor")).extracting(ComponentCandidateDTO::getVariantId)
                .startsWith(202, 201, 203).endsWith(210);
    }

    @Test
    @DisplayName("budgetRange: tổng 3 món rẻ nhất và đắt nhất")
    void budgetRangeSumsCheapestAndMostExpensive() {
        PeripheralSelector.BudgetRange range = PeripheralSelector.budgetRange(catalog());

        assertPrice(range.min(), 2_800_000);
        assertPrice(range.max(), 36_370_000);
    }

    @ParameterizedTest(name = "thiếu {0}")
    @CsvSource({"monitor", "keyboard", "mouse"})
    @DisplayName("kho thiếu một loại: budgetRange null, select báo PERIPHERALS_UNAVAILABLE")
    void missingRoleIsUnavailable(String missing) {
        List<ComponentCandidateDTO> items = catalog().stream()
                .filter(item -> !missing.equals(item.getCategorySlug())).toList();

        assertThat(PeripheralSelector.budgetRange(items)).isNull();
        assertThatThrownBy(() -> select(items, 30_000_000, 23_000_000, "GENERAL"))
                .isInstanceOfSatisfying(BuildRuleException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("PERIPHERALS_UNAVAILABLE");
                    assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                });
    }

    @Test
    @DisplayName("kho rỗng: budgetRange null")
    void emptyCatalogHasNoRange() {
        assertThat(PeripheralSelector.budgetRange(List.of())).isNull();
    }

    @ParameterizedTest(name = "ngân sách {0}, thùng {1}, thiếu {2}")
    @CsvSource({
            "12000000, 9200001, 1",          // thiếu đúng 1 đồng
            "12000000, 12000000, 2800000",   // không còn đồng nào
            "12000000, 15000000, 5800000",   // thùng máy đã vượt ngân sách
    })
    @DisplayName("tiền còn lại dưới tổng 3 món rẻ nhất: PERIPHERAL_BUDGET_TOO_LOW kèm số tiền thiếu")
    void rejectsWhenNotEnoughLeft(long budget, long pcTotal, long shortfall) {
        assertThatThrownBy(() -> select(catalog(), budget, pcTotal, "GENERAL"))
                .isInstanceOfSatisfying(BuildRuleException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("PERIPHERAL_BUDGET_TOO_LOW");
                    assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    @SuppressWarnings("unchecked")
                    Map<String, BigDecimal> data = (Map<String, BigDecimal>) error.getData();
                    assertPrice(data.get("shortfall"), shortfall);
                    assertPrice(data.get("minTotal"), 2_800_000);
                });
    }
}
