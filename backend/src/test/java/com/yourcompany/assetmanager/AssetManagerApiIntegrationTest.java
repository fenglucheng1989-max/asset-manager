package com.yourcompany.assetmanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssetManagerApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginAndRejectInvalidPassword() throws Exception {
        String username = uniqueUsername();
        String token = register(username, "pass123456");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "pass123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        mockMvc.perform(get("/api/v1/asset/accounts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void legalDocumentsAreRequiredForRegistration() throws Exception {
        mockMvc.perform(get("/api/v1/legal-documents/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.terms.version").value("2026.04.28"))
                .andExpect(jsonPath("$.data.privacy.version").value("2026.04.28"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", uniqueUsername(),
                                "password", "pass123456",
                                "email", "noaccept@test.local"
                        ))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", uniqueUsername(),
                                "password", "pass123456",
                                "email", "oldversion@test.local",
                                "acceptLegal", true,
                                "acceptedTermsVersion", "old",
                                "acceptedPrivacyVersion", "old"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void accountCrudSortAndOverviewWorkTogether() throws Exception {
        String token = register(uniqueUsername(), "pass123456");

        long bankId = createAccount(token, Map.of(
                "name", "工资卡",
                "accountType", "BANK",
                "currency", "USD",
                "exchangeRateToCny", new BigDecimal("7.00"),
                "currentBalance", new BigDecimal("1000.50"),
                "isLiability", false,
                "includeInTotal", true,
                "colorHex", "#2EBD85"
        ));

        long creditCardId = createAccount(token, Map.of(
                "name", "信用卡",
                "accountType", "CREDIT_CARD",
                "currentBalance", new BigDecimal("200.25"),
                "isLiability", true,
                "includeInTotal", true,
                "colorHex", "#FF6B6B"
        ));

        mockMvc.perform(get("/api/v1/asset/accounts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(creditCardId))
                .andExpect(jsonPath("$.data[1].id").value(bankId));

        mockMvc.perform(get("/api/v1/asset/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAsset", comparesEqualTo(7003.50)))
                .andExpect(jsonPath("$.data.totalLiability", comparesEqualTo(200.25)))
                .andExpect(jsonPath("$.data.netWorth", comparesEqualTo(6803.25)))
                .andExpect(jsonPath("$.data.accountCount").value(2));

        mockMvc.perform(put("/api/v1/asset/accounts/" + bankId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "工资卡更新",
                                "accountType", "BANK",
                                "currency", "USD",
                                "exchangeRateToCny", new BigDecimal("7.00"),
                                "currentBalance", new BigDecimal("1200.00"),
                                "isLiability", false,
                                "includeInTotal", true,
                                "colorHex", "#2EBD85"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("工资卡更新"));

        mockMvc.perform(get("/api/v1/asset/accounts/" + bankId + "/history?limit=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].changeType").value("MANUAL_ADJUST"))
                .andExpect(jsonPath("$.data[0].beforeBalance", comparesEqualTo(1000.50)))
                .andExpect(jsonPath("$.data[0].afterBalance", comparesEqualTo(1200.00)));

        mockMvc.perform(put("/api/v1/asset/accounts/sort")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("sortedIds", new long[]{creditCardId, bankId}))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/asset/accounts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(creditCardId))
                .andExpect(jsonPath("$.data[1].id").value(bankId));

        mockMvc.perform(delete("/api/v1/asset/accounts/" + creditCardId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/asset/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAsset", comparesEqualTo(8400.00)))
                .andExpect(jsonPath("$.data.totalLiability", comparesEqualTo(0)))
                .andExpect(jsonPath("$.data.netWorth", comparesEqualTo(8400.00)))
                .andExpect(jsonPath("$.data.accountCount").value(1));
    }

    @Test
    void accountArchiveRemovesAccountFromListAndOverview() throws Exception {
        String token = register(uniqueUsername(), "pass123456");

        long accountId = createAccount(token, Map.of(
                "name", "待归档账户",
                "accountType", "BANK",
                "currentBalance", new BigDecimal("888.00"),
                "isLiability", false,
                "includeInTotal", true,
                "colorHex", "#2EBD85"
        ));

        mockMvc.perform(put("/api/v1/asset/accounts/" + accountId + "/archive")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("archived", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.archived").value(true));

        mockMvc.perform(get("/api/v1/asset/accounts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/v1/asset/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAsset", comparesEqualTo(0)))
                .andExpect(jsonPath("$.data.accountCount").value(0));
    }

    @Test
    void userCanSetBaseCurrency() throws Exception {
        String token = register(uniqueUsername(), "pass123456");

        mockMvc.perform(get("/api/v1/user/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseCurrency").value("CNY"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());

        mockMvc.perform(put("/api/v1/user/profile/base-currency")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("baseCurrency", "USD"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseCurrency").value("USD"));
    }

    @Test
    void snapshotCanBeCreatedUpdatedAndListed() throws Exception {
        String token = register(uniqueUsername(), "pass123456");

        long accountId = createAccount(token, Map.of(
                "name", "投资账户",
                "accountType", "INVESTMENT",
                "currentBalance", new BigDecimal("5000.00"),
                "isLiability", false,
                "includeInTotal", true,
                "colorHex", "#5B8FF9"
        ));

        mockMvc.perform(post("/api/v1/asset/snapshots")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshotDate").isNotEmpty())
                .andExpect(jsonPath("$.data.totalAsset", comparesEqualTo(5000.00)))
                .andExpect(jsonPath("$.data.totalLiability", comparesEqualTo(0)))
                .andExpect(jsonPath("$.data.netWorth", comparesEqualTo(5000.00)));

        mockMvc.perform(put("/api/v1/asset/accounts/" + accountId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "投资账户",
                                "accountType", "INVESTMENT",
                                "currentBalance", new BigDecimal("6800.00"),
                                "isLiability", false,
                                "includeInTotal", true,
                                "colorHex", "#5B8FF9"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/asset/snapshots")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAsset", comparesEqualTo(6800.00)))
                .andExpect(jsonPath("$.data.netWorth", comparesEqualTo(6800.00)));

        mockMvc.perform(get("/api/v1/asset/snapshots?limit=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].totalAsset", comparesEqualTo(6800.00)));
    }

    @Test
    void accountCenterCanExportAndClearUserData() throws Exception {
        String token = register(uniqueUsername(), "pass123456");

        long accountId = createAccount(token, Map.of(
                "name", "导出账户",
                "accountType", "BANK",
                "currentBalance", new BigDecimal("321.00"),
                "isLiability", false,
                "includeInTotal", true,
                "colorHex", "#2EBD85"
        ));

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "transactionType", "INCOME",
                                "accountId", accountId,
                                "amount", new BigDecimal("79.00"),
                                "currency", "CNY",
                                "exchangeRateToCny", BigDecimal.ONE
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/asset/snapshots")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/user/data/export/accounts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("账户ID,账户名称,账户类型")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("导出账户")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("银行卡")));

        mockMvc.perform(get("/api/v1/user/data/export/transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("流水ID,类型,账户ID")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("收入")));

        String backupResponse = mockMvc.perform(get("/api/v1/user/data/backup")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accounts", hasSize(1)))
                .andExpect(jsonPath("$.data.transactions", hasSize(1)))
                .andExpect(jsonPath("$.data.snapshots", hasSize(1)))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode backup = objectMapper.readTree(backupResponse).path("data");

        mockMvc.perform(delete("/api/v1/user/data")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/asset/accounts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/v1/asset/snapshots?limit=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(post("/api/v1/user/data/restore")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(backup)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/asset/accounts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("导出账户"));

        mockMvc.perform(get("/api/v1/transactions?limit=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        mockMvc.perform(get("/api/v1/asset/snapshots?limit=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void accountCenterCanChangePasswordAndDeleteAccount() throws Exception {
        String username = uniqueUsername();
        String token = register(username, "pass123456");

        mockMvc.perform(put("/api/v1/user/security/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "currentPassword", "pass123456",
                                "newPassword", "next123456"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "pass123456"))))
                .andExpect(status().isUnauthorized());

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "next123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String nextToken = objectMapper.readTree(loginResponse).path("data").path("token").asText();
        mockMvc.perform(delete("/api/v1/user/account")
                        .header("Authorization", "Bearer " + nextToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", "next123456"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "next123456"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCanSubmitFeedback() throws Exception {
        String token = register(uniqueUsername(), "pass123456");

        mockMvc.perform(post("/api/v1/user/feedback")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "type", "EXPORT",
                                "content", "CSV 表头希望使用中文业务名称",
                                "contact", "tester@example.com"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/user/feedback")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "type", "BUG",
                                "content", ""
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transactionRecordsDriveBalancesAndCanBeReversed() throws Exception {
        String token = register(uniqueUsername(), "pass123456");

        long accountId = createAccount(token, Map.of(
                "name", "现金账户",
                "accountType", "CASH",
                "currentBalance", new BigDecimal("1000.00"),
                "isLiability", false,
                "includeInTotal", true,
                "colorHex", "#2EBD85"
        ));

        String expenseResponse = mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "transactionType", "EXPENSE",
                                "accountId", accountId,
                                "amount", new BigDecimal("120.50"),
                                "currency", "CNY",
                                "exchangeRateToCny", BigDecimal.ONE,
                                "tag", "午餐"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionType").value("EXPENSE"))
                .andExpect(jsonPath("$.data.amount", comparesEqualTo(120.50)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long recordId = objectMapper.readTree(expenseResponse).path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/asset/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAsset", comparesEqualTo(879.50)))
                .andExpect(jsonPath("$.data.netWorth", comparesEqualTo(879.50)));

        mockMvc.perform(get("/api/v1/asset/accounts/" + accountId + "/history?limit=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].changeType").value("TRANSACTION_EXPENSE"))
                .andExpect(jsonPath("$.data[0].beforeBalance", comparesEqualTo(1000.00)))
                .andExpect(jsonPath("$.data[0].afterBalance", comparesEqualTo(879.50)));

        mockMvc.perform(get("/api/v1/transactions?limit=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].accountName").value("现金账户"));

        mockMvc.perform(delete("/api/v1/transactions/" + recordId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/asset/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAsset", comparesEqualTo(1000.00)))
                .andExpect(jsonPath("$.data.netWorth", comparesEqualTo(1000.00)));
    }

    @Test
    void transferRecordMovesBalanceBetweenAccounts() throws Exception {
        String token = register(uniqueUsername(), "pass123456");

        long fromId = createAccount(token, Map.of(
                "name", "银行卡",
                "accountType", "BANK",
                "currentBalance", new BigDecimal("3000.00"),
                "isLiability", false,
                "includeInTotal", true,
                "colorHex", "#2EBD85"
        ));
        long toId = createAccount(token, Map.of(
                "name", "零钱",
                "accountType", "CASH",
                "currentBalance", new BigDecimal("200.00"),
                "isLiability", false,
                "includeInTotal", true,
                "colorHex", "#5B8FF9"
        ));

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "transactionType", "TRANSFER",
                                "accountId", fromId,
                                "targetAccountId", toId,
                                "amount", new BigDecimal("600.00"),
                                "currency", "CNY",
                                "exchangeRateToCny", BigDecimal.ONE
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionType").value("TRANSFER"))
                .andExpect(jsonPath("$.data.accountName").value("银行卡"))
                .andExpect(jsonPath("$.data.targetAccountName").value("零钱"));

        mockMvc.perform(get("/api/v1/asset/accounts/" + fromId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBalance", comparesEqualTo(2400.00)));

        mockMvc.perform(get("/api/v1/asset/accounts/" + toId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBalance", comparesEqualTo(800.00)));

        mockMvc.perform(get("/api/v1/asset/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAsset", comparesEqualTo(3200.00)));
    }

    @Test
    void transactionEnhancementsSupportCategoryBudgetFilterAndReport() throws Exception {
        String token = register(uniqueUsername(), "pass123456");

        long accountId = createAccount(token, Map.of(
                "name", "日常账户",
                "accountType", "BANK",
                "currentBalance", new BigDecimal("2000.00"),
                "isLiability", false,
                "includeInTotal", true,
                "colorHex", "#2EBD85"
        ));

        String categoryResponse = mockMvc.perform(post("/api/v1/transactions/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "咖啡",
                                "type", "EXPENSE",
                                "colorHex", "#8B5CF6",
                                "sortOrder", 5
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("咖啡"))
                .andExpect(jsonPath("$.data.systemDefault").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long categoryId = objectMapper.readTree(categoryResponse).path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "transactionType", "EXPENSE",
                                "accountId", accountId,
                                "categoryId", categoryId,
                                "amount", new BigDecimal("88.00"),
                                "currency", "CNY",
                                "exchangeRateToCny", BigDecimal.ONE,
                                "occurredAt", "2026-04-28T10:00:00"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/transactions/budgets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "budgetMonth", "2026-04",
                                "categoryId", categoryId,
                                "budgetType", "EXPENSE",
                                "amount", new BigDecimal("100.00"),
                                "warningRate", new BigDecimal("0.80")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.usedAmount", comparesEqualTo(88.00)))
                .andExpect(jsonPath("$.data.warning").value(true));

        mockMvc.perform(get("/api/v1/transactions?categoryId=" + categoryId + "&startDate=2026-04-01&endDate=2026-04-30")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].categoryName").value("咖啡"));

        mockMvc.perform(get("/api/v1/transactions/report?month=2026-04")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expense", comparesEqualTo(88.00)))
                .andExpect(jsonPath("$.data.net", comparesEqualTo(-88.00)))
                .andExpect(jsonPath("$.data.categoryStats[0].categoryName").value("咖啡"));
    }

    @Test
    void financialInsightProvidesHealthSummaryAndMilestones() throws Exception {
        String token = register(uniqueUsername(), "pass123456");

        long accountId = createAccount(token, Map.of(
                "name", "稳健账户",
                "accountType", "BANK",
                "currentBalance", new BigDecimal("150000.00"),
                "isLiability", false,
                "includeInTotal", true,
                "colorHex", "#2EBD85"
        ));

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "transactionType", "EXPENSE",
                                "accountId", accountId,
                                "amount", new BigDecimal("3000.00"),
                                "currency", "CNY",
                                "exchangeRateToCny", BigDecimal.ONE
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "transactionType", "INCOME",
                                "accountId", accountId,
                                "amount", new BigDecimal("9000.00"),
                                "currency", "CNY",
                                "exchangeRateToCny", BigDecimal.ONE
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/asset/snapshots")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/insight/health-score")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").isNumber())
                .andExpect(jsonPath("$.data.grade").isNotEmpty())
                .andExpect(jsonPath("$.data.dataInsufficient").value(false))
                .andExpect(jsonPath("$.data.metrics", hasSize(4)));

        mockMvc.perform(get("/api/v1/insight/summaries?limit=3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].reportType").value("MONTHLY"))
                .andExpect(jsonPath("$.data[0].income", comparesEqualTo(9000.00)))
                .andExpect(jsonPath("$.data[0].expense", comparesEqualTo(3000.00)))
                .andExpect(jsonPath("$.data[0].comparisonAvailable").value(false))
                .andExpect(jsonPath("$.data[0].dataNotice").value("基于已记录流水生成，未记录的现金流不会纳入统计。"));

        mockMvc.perform(get("/api/v1/insight/milestones")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].milestoneType").value("NET_WORTH"));

        mockMvc.perform(post("/api/v1/insight/milestones")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("note", "还清车贷"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.milestoneType").value("CUSTOM"))
                .andExpect(jsonPath("$.data.note").value("还清车贷"));
    }

    private String register(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "password", password,
                                "email", username + "@test.local",
                                "acceptLegal", true,
                                "acceptedTermsVersion", "2026.04.28",
                                "acceptedPrivacyVersion", "2026.04.28"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private long createAccount(String token, Map<String, Object> payload) throws Exception {
        String response = mockMvc.perform(post("/api/v1/asset/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode body = objectMapper.readTree(response);
        return body.path("data").path("id").asLong();
    }

    @Test
    void yearlyBudgetSavesAndListsCorrectly() throws Exception {
        String token = register(uniqueUsername(), "pass123456");
        long categoryId = createCategory(token, "旅行", "EXPENSE", "#FF6B6B");

        mockMvc.perform(post("/api/v1/transactions/budgets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "budgetMonth", "2026",
                                "periodType", "YEARLY",
                                "categoryId", categoryId,
                                "budgetType", "EXPENSE",
                                "amount", new BigDecimal("50000.00"),
                                "warningRate", new BigDecimal("0.80")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.periodType").value("YEARLY"))
                .andExpect(jsonPath("$.data.budgetMonth").value("2026"))
                .andExpect(jsonPath("$.data.amount", comparesEqualTo(50000.00)));

        mockMvc.perform(get("/api/v1/transactions/budgets?periodType=YEARLY&periodKey=2026")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].periodType").value("YEARLY"));
    }

    private long createCategory(String token, String name, String type, String colorHex) throws Exception {
        String response = mockMvc.perform(post("/api/v1/transactions/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", name,
                                "type", type,
                                "colorHex", colorHex,
                                "sortOrder", 5
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private String uniqueUsername() {
        return "u_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
