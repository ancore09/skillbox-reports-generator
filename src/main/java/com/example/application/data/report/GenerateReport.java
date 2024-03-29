package com.example.application.data.report;

import com.example.application.data.entity.SampleContractor;
import com.example.application.data.entity.SampleReport;
import com.example.application.data.entity.User;
import com.example.application.database.DBManager;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlException;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class GenerateReport {

    private final SampleContractor contractor;
    private final SampleReport report;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;

    private final NumberFormat nf = NumberFormat.getInstance(new Locale("sk", "SK"));
    private final DecimalFormat df = new DecimalFormat("0.00");

    private final ExcelParsing parsing;

    private static User authUser;

    private String rewardResult;

    private double oa;

    public GenerateReport(SampleContractor contractor, SampleReport report,
                          LocalDate periodStart, LocalDate periodEnd) {
        this.contractor = contractor;
        this.report = report;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        parsing = new ExcelParsing(periodStart.getMonthValue(), Integer.parseInt(report.getCourseCode()));

        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
    }

    public static void setAuthenticatedUser(User user) {
        authUser = user;
    }

    public int createTemplateForContractor() throws InvalidFormatException, IOException, XmlException {
        XWPFDocument doc = new XWPFDocument();
        FileProvider provider = new FileProvider();
        String filePath = "Шаблоны. " + report.getReportModel() + "/";
        switch (contractor.getContractorType()) {
            case "Индивидуальный предприниматель":
                doc = provider.readDocxFileFromDropbox(filePath + "Шаблон_ИП.docx");
                break;
            case "Самозанятый":
                doc = provider.readDocxFileFromDropbox(filePath + "Шаблон_СЗ.docx");
                break;
            case "Физическое лицо":
                doc = provider.readDocxFileFromDropbox(filePath + "Шаблон_Физлицо.docx");
                break;
            case "Юридическое лицо":
                doc = provider.readDocxFileFromDropbox(filePath + "Шаблон_Юрлицо.docx");
                break;
        }
        createNewDocument(doc, provider);

        DBManager.insertRowIntoLogs(LocalDate.now(), authUser.getName(), Integer.parseInt(report.getCourseCode()),
                contractor, periodStart + " - " + periodEnd);
        return 1;
    }

    private void createNewDocument(XWPFDocument doc, FileProvider provider)
            throws IOException, InvalidFormatException, XmlException {
        for (XWPFParagraph p : doc.getParagraphs()) {
            List<XWPFRun> runs = p.getRuns();
            if (runs != null) {
                for (XWPFRun r : runs) {
                    String text = r.getText(0);
                    r.setText(text, 0);
                }
            }
        }

        String filePath = contractor.getLastName() + " " +
                WordsConverter.convertNumericMonthToString(periodStart.getMonthValue()) + " " +
                report.getCourseCode() + ".docx";
        doc.write(new FileOutputStream(filePath));
        XWPFDocument copy = new XWPFDocument(OPCPackage.open(filePath));

        parsing.parseExcelTable();
        int addRowsNumber = parsing.getRowsNumber();
        List<List<String>> partnerCoursesProceeds = parsing.getPartnerCoursesProceeds();
        List<List<String>> partnerCoursesRefunds = parsing.getPartnerCoursesRefunds();
        List<List<String>> partnerCoursesShares = parsing.getPartnerCoursesShares();

        insertRowsInTables(copy, 2, addRowsNumber, partnerCoursesProceeds);
        insertRowsInTables(copy, 3, addRowsNumber, partnerCoursesRefunds);
        insertRowsInTables(copy, report.getReportModel().equals("Чистая выручка") ? 4 : 5,
                addRowsNumber, partnerCoursesShares);

        double F = parsing.getProceedTotalsSum() - parsing.getRefundTotalsSum();
        String correctReward = "";

        for (XWPFTable tbl : copy.getTables()) {
            for (XWPFTableRow row : tbl.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        for (XWPFRun r : p.getRuns()) {
                            String text = r.getText(0);

                            if (report.getReportModel().equals("Модель К2"))
                                text = updateK2Table(text);

                            if (correctReward.equals(""))
                                correctReward = getCorrectReward(F);

                            text = updateCommonTables(text, partnerCoursesProceeds, partnerCoursesRefunds,
                                    partnerCoursesShares, correctReward, F);

                            r.setText(text, 0);
                        }
                    }
                }
            }
        }

        for (XWPFParagraph p : copy.getParagraphs()) {
            List<XWPFRun> runs = p.getRuns();
            if (runs != null) {
                for (XWPFRun r : runs) {
                    String text = r.getText(0);
                    r.setText(updateMainText(text, correctReward, getRubles()), 0);
                }
            }
        }

        String contractorName = contractor.getLastName() + " " +
                contractor.getFirstName() + " " + contractor.getSecondName();

        switch (contractor.getContractorType()) {
            case "Индивидуальный предприниматель":
                fillFieldsForIndividualEntrepreneur(copy, contractorName);
                break;
            case "Самозанятый":
                fillFieldsForSelfEmployed(copy, contractorName);
                break;
            case "Физическое лицо":
                fillFieldsForIndividual(copy, contractorName);
                break;
            case "Юридическое лицо":
                fillFieldsForLegalEntity(copy);
                break;
        }

        String folderName = "/Сформированные отчёты/";
        boolean fileExists = provider.checkFileExistence(folderName + filePath);
        if (fileExists)
            provider.deleteFileFromDropbox(folderName + filePath);
        provider.uploadFile(copy, folderName + filePath);
        copy.close();
    }

    private String updateMainText(String text, String rewardResult, int rubles) {
        List<String> rewardSplitted = Arrays.asList(rewardResult.split(","));

        text = replaceTag(text, "CourseName", report.getCourseName());
        text = replaceTag(text, "ContractNumber", report.getContractNumber());
        text = replaceTag(text, "ContractDateDay",
                WordsConverter.getDayOfMonth(report.getContractDate().getDayOfMonth()));
        text = replaceTag(text, "ContractDateMonth",
                WordsConverter.convertGenetiveNumericMonthToString(report.getContractDate().getMonthValue()));
        text = replaceTag(text, "ContractDateYear",
                "" + report.getContractDate().getYear());

        text = replaceTag(text, "PeriodBeginning", WordsConverter.getDayOfMonth(periodStart.getDayOfMonth()));
        text = replaceTag(text, "PeriodEnding", WordsConverter.getDayOfMonth(periodEnd.getDayOfMonth()));
        text = replaceTag(text, "PeriodMonth",
                WordsConverter.convertGenetiveNumericMonthToString(periodStart.getMonthValue()));
        text = replaceTag(text, "PeriodYear", "" + periodStart.getYear());
        text = replaceTag(text, "CourseCode", report.getCourseCode());
        text = replaceTag(text, "CourseDirection", report.getCourseDirection());
        text = replaceTag(text, "ContractObject", report.getCourseObject().contains("Программа") ?
                report.getCourseObject().replace("Программа", "Программу") :
                report.getCourseObject());

        text = replaceTag(text, "RewardRubles", rewardSplitted.get(0) +
                " (" + WordsConverter.convertNumericRublesToString(rubles) + ")" +
                WordsConverter.convertRublesToGenitive(rewardSplitted.get(0)));
        text = replaceTag(text, "RewardCoins",
                WordsConverter.convertCoinsToGenitive(rewardSplitted.get(1)));

        text = changeSigner(text);

        return text;
    }

    private String updateCommonTables(String text, List<List<String>> partnerCoursesProceeds,
                                      List<List<String>> partnerCoursesRefunds,
                                      List<List<String>> partnerCoursesShares,
                                      String rewardResult, double F) {
        text = replaceTag(text, "PeriodBeginning", WordsConverter.getDayOfMonth(periodStart.getDayOfMonth()));
        text = replaceTag(text, "PeriodEnding", WordsConverter.getDayOfMonth(periodEnd.getDayOfMonth()));
        text = replaceTag(text, "PeriodMonth",
                WordsConverter.convertGenetiveNumericMonthToString(periodStart.getMonthValue()));
        text = replaceTag(text, "PeriodYear", "" + periodStart.getYear());

        text = replaceTag(text, "RoyaltyPercentage", getCorrectDoubleValue(report.getRoyaltyPercentage()));
        text = replaceTag(text, "PartnerCourse", partnerCoursesShares.get(0).get(0));
        text = replaceTag(text, "CourseShare", partnerCoursesShares.get(0).get(1));
        text = replaceTag(text, "CourseProceed", partnerCoursesProceeds.get(0).get(1));
        text = replaceTag(text, "CourseProcRes", partnerCoursesProceeds.get(0).get(3));
        text = replaceTag(text, "CourseRefund", partnerCoursesRefunds.get(0).get(1));
        text = replaceTag(text, "CourseRefRes", partnerCoursesRefunds.get(0).get(3));

        text = replaceTag(text, "ProceedsSum", nf.format(parsing.getProceedsSum()));
        text = replaceTag(text, "ProceedsResult", nf.format(parsing.getProceedTotalsSum()));
        text = replaceTag(text, "RefundsSum", nf.format(parsing.getRefundsSum()));
        text = replaceTag(text, "RefundsResult", nf.format(parsing.getRefundTotalsSum()));
        text = replaceTag(text, "ProceedsWithoutRefunds", nf.format(F));
        df.setRoundingMode(RoundingMode.UP);
        text = replaceTag(text, "Reward", rewardResult);

        text = replaceTag(text, "FullName",
                contractor.getFirstName().charAt(0) + ". " +
                        contractor.getSecondName().charAt(0) + ". " +
                        contractor.getLastName());

        text = changeSigner(text);

        return text;
    }

    private String updateK2Table(String text) {
        text = replaceTag(text, "K2Indicator", getCorrectDoubleValue(report.getK2()));
        text = replaceTag(text, "K2Sum", nf.format(parsing.getProceedTotalsSum() *
                Double.parseDouble(report.getK2().replace(',', '.')) / 100));

        text = replaceTag(text, "ExecutorHWCosts", nf.format(parsing.getExecutorHWCostsSum()));
        text = replaceTag(text, "ExecutorDiplomaCosts", nf.format(parsing.getExecutorDiplomaCostsSum()));
        double ob = parsing.getExecutorHWCostsSum() + parsing.getExecutorDiplomaCostsSum();
        text = replaceTag(text, "ExecutorCostsSum", nf.format(ob));

        int customerHWNum = parsing.getCustomerHWNumber();
        int executorHWNum = parsing.getExecutorHWNumber();
        int customerDiplomaNum = parsing.getCustomerDiplomaNumber();
        int executorDiplomaNum = parsing.getExecutorDiplomaNumber();
        text = changePrices(parsing, text);
        text = replaceTag(text, "ExecutorHWPrice",
                getPricesForK2(parsing.getExecutorHWCostsSum(), executorHWNum, 200));
        text = replaceTag(text, "ExecutorDiplomaPrice",
                getPricesForK2(parsing.getExecutorDiplomaCostsSum(), executorDiplomaNum, 1000));

        text = replaceTag(text, "PeriodBeginning", WordsConverter.getDayOfMonth(periodStart.getDayOfMonth()));
        text = replaceTag(text, "PeriodEnding", WordsConverter.getDayOfMonth(periodEnd.getDayOfMonth()));
        text = replaceTag(text, "PeriodMonthK2", WordsConverter.getDayOfMonth(periodStart.getMonthValue()));
        text = replaceTag(text, "PeriodYear", Integer.toString(periodStart.getYear()));

        text = replaceTag(text, "HWNum", Integer.toString(customerHWNum + executorHWNum));
        text = replaceTag(text, "DiplomaNum", Integer.toString(customerDiplomaNum + executorDiplomaNum));

        double s = parsing.getProceedTotalsSum();
        double k2 = (100 - Double.parseDouble(report.getK2())) / 100;
        double c = parsing.getRefundTotalsSum();
        double finalResult = s * k2 - c - oa - ob;
        double royalty = Double.parseDouble(report.getRoyaltyPercentage()) / 100;

        double roundedResult = BigDecimal.valueOf(finalResult).setScale(2, RoundingMode.HALF_UP).doubleValue();
        double roundedFinal = BigDecimal.valueOf(roundedResult * royalty).setScale(2, RoundingMode.HALF_UP).doubleValue();
        rewardResult = df.format(roundedFinal);
        text = replaceTag(text, "FinalRew", nf.format(roundedFinal));

        text = replaceTag(text, "Final", nf.format(finalResult));

        return text;
    }

    private String changeSigner(String text) {
        String initials;
        String proxyNumber;
        String proxyDate;

        StringBuilder sbSigner = new StringBuilder();
        sbSigner.append("в лице ");
        // NEW //
        if (report.isSignedEdo()) {
            sbSigner.append("Генерального директора Крутова Дмитрия Валерьевича");
            initials = "Д.В. Крутов";
            sbSigner.append(", действующего на основании Устава");

            text = replaceTag(text, "SignerProxy", "Генеральный директор");
            text = replaceTag(text, "SignerAddInfo", "ООО «СКИЛБОКС»");
            text = replaceTag(text, "SignerInitials", initials);
            text = replaceTag(text, "Signer", sbSigner.toString());
            return text;
        }

        switch (report.getCourseDirection()) {
            case "Программирование":
                sbSigner.append("Сырычко Александра Васильевича");
                initials = "А.В. Сырычко";
                proxyNumber = "242";
                proxyDate = "22.04.2022";
                break;
            case "Дизайн":
                //case "Мультимедиа":
                sbSigner.append("Климова Антона Александровича");
                initials = "А.А. Климов";
                proxyNumber = "261";
                proxyDate = "29.07.2022";
                break;
            case "Игры":
                //case "Маркетинг":
                sbSigner.append("Татьянкина Дениса Александровича");
                initials = "Д.А. Татьянкин";
                proxyNumber = "262";
                proxyDate = "29.07.2022";
                break;
            case "Управление":
                sbSigner.append("Прудникова Кирилла Павловича");
                initials = "К.П. Прудников";
                proxyNumber = "228";
                proxyDate = "15.02.2022";
                break;
            default:
                sbSigner.append("Копылова Антона Сергеевича");
                initials = "А.С. Копылов";
                proxyNumber = "210";
                proxyDate = "31.12.2021";
                break;
        }
        sbSigner.append(", действующего на основании Доверенности №")
                .append(proxyNumber).append(" от ")
                .append(proxyDate).append(" г.");

        String proxy = "на основании доверенности №" + proxyNumber + " от " + proxyDate + " г.";

        text = replaceTag(text, "SignerProxy", proxy);
        text = replaceTag(text, "SignerAddInfo", "");
        text = replaceTag(text, "SignerInitials", initials);
        text = replaceTag(text, "Signer", sbSigner.toString());

        return text;
    }

    private String changePrices(ExcelParsing parsing, String text) {
        int hwPrice = 200;
        int diplomaPrice = 1000;
        int hwSum;
        int diplomaSum;

        if (checkByContractorsName("Новожилов", "Юрий", "Вячеславович", "298")
                || checkByContractorsName("Мардамшин", "Наиль", "Ришатович", "181")) {
            hwPrice = 200;
            diplomaPrice = 1000;
        } else if (checkByContractorsName("Синицына", "Наиля", "Вадимовна", "304")
                || checkByContractorsName("Анцукевич", "Сергей", "Юрьевич", "291")
                || checkByContractorsName("Анцукевич", "Сергей", "Юрьевич", "332")
                || checkByContractorsName("Цыганов", "Роман", "Александрович", "207")
                || checkByContractorsName("Гладышева", "Дарья", "Михайловна", "314")
                || checkByContractorsName("Зернов", "Илья", "Александрович", "312")
                || checkByContractorsName("Зернов", "Илья", "Александрович", "333")
                || checkByContractorsName("Казачков", "Евгений", "Борисович", "274")
                || checkByContractorsName("Дозорцев", "Вадим", "Юльевич", "106")
                || checkByContractorsName("Крутова", "Александра", "Сергеевна", "384")
                || checkByContractorsName("Марчук", "Анна", "Петровна", "387")
                || checkByContractorsName("Пискарева", "Виктория", "Алексеевна", "345")
                || checkByContractorsName("Соколова", "Анна", "Аркадьевна", "146")
                || checkByContractorsName("Юрчук", "Анна", "Александровна", "318")) {
            hwPrice = 300;
            diplomaPrice = 2000;
        } else if (checkByContractorsName("Орлов", "Петр", "Сергеевич", "175")) {
            hwPrice = 0;
            diplomaPrice = 0;
        } else if (contractor.getOOOName() != null) {
            if (checkByOOOName("ОМАЙБРЕНД", "319")
                    || checkByOOOName("СПОРТ МЕДИА", "296")
                    || checkByOOOName("МОСКОВСКИЙ ГОСУДАРСТВЕННЫЙ УНИВЕРСИТЕТ ИМЕНИ М.В.ЛОМОНОСОВА, МГУ ИМЕНИ М.В.ЛОМОНОСОВА, МОСКОВСКИЙ УН", "349")
                    || checkByOOOName("АРТОКС МЕДИА", "471")
                    || checkByOOOName("ОСМЫСЛЕННО", "413")
                    || checkByOOOName("ПИ.АР. - ТЕКНОЛОДЖИС", "350")
                    || checkByOOOName("СТУДИЯ Д. КУПОВЫХ", "398")
                    || checkByOOOName("СТУДИЯ Д. КУПОВЫХ", "399")
                    || checkByOOOName("СТУДИЯ Д. КУПОВЫХ", "406")
                    || checkByOOOName("СТУДИЯ Д. КУПОВЫХ", "407")
                    || checkByOOOName("СТУДИЯ Д. КУПОВЫХ", "423")
                    || checkByOOOName("АДВЕНТУМ КОНСАЛТИНГ", "372")) {
                hwPrice = 300;
                diplomaPrice = 2000;
            } else if (checkByOOOName("ПИ.АР. - ТЕКНОЛОДЖИС", "220")) {
                hwPrice = 240;
                diplomaPrice = 1200;
            } else if (checkByOOOName("ЛОГОМАШИНА", "116")) {
                hwPrice = 200;
                diplomaPrice = 0;
            }
        } else {
            text = replaceTag(text, "CustomerHWPrice", getPricesForK2(parsing.getCustomerHWCostsSum(), parsing.getCustomerHWNumber(), 200));
            text = replaceTag(text, "CustomerDiplomaPrice", getPricesForK2(parsing.getCustomerDiplomaCostsSum(), parsing.getCustomerDiplomaNumber(), 1000));
            text = replaceTag(text, "CustomerHWCosts", nf.format(parsing.getCustomerHWCostsSum()));
            text = replaceTag(text, "CustomerDiplomaCosts", nf.format(parsing.getCustomerDiplomaCostsSum()));
            oa = parsing.getCustomerHWCostsSum() + parsing.getCustomerDiplomaCostsSum();
            text = replaceTag(text, "CustomerCostsSum", nf.format(oa));
            return text;
        }

        hwSum = countSumForHWAndDiploma(hwPrice, parsing.getCustomerHWNumber());
        diplomaSum = countSumForHWAndDiploma(diplomaPrice, parsing.getCustomerDiplomaNumber());
        oa = hwSum + diplomaSum;

        text = replaceTag(text, "CustomerHWPrice", Integer.toString(hwPrice));
        text = replaceTag(text, "CustomerDiplomaPrice", Integer.toString(diplomaPrice));
        text = replaceTag(text, "CustomerHWCosts", nf.format(hwSum));
        text = replaceTag(text, "CustomerDiplomaCosts", nf.format(diplomaSum));

        text = replaceTag(text, "CustomerCostsSum", nf.format(oa));
        return text;
    }

    private boolean checkByContractorsName(String lastName, String firstName, String secondName, String courseCode) {
        return contractor.getLastName().equals(lastName) && contractor.getFirstName().equals(firstName)
                && contractor.getSecondName().equals(secondName) && report.getCourseCode().equals(courseCode);
    }

    private boolean checkByOOOName(String OOOName, String courseCode) {
        return contractor.getOOOName().equals(OOOName) && report.getCourseCode().equals(courseCode);
    }

    private int countSumForHWAndDiploma(int price, int number) {
        return price * number;
    }

    private String getPricesForK2(double sum, int num, int defaultValue) {
        int price = 0;
        if (num != 0) {
            price = (int) (sum / num);
            return Integer.toString(price);
        } else return Integer.toString(defaultValue);
    }

    private String getCorrectDoubleValue(String value) {
        if (value.matches("^\\d*.0$"))
            value = value.substring(0, value.indexOf("."));
        return value;
    }

    private void fillFieldsForIndividual(XWPFDocument copy, String contractorName) {
        for (XWPFParagraph p : copy.getParagraphs()) {
            List<XWPFRun> runs = p.getRuns();
            if (runs != null) {
                for (XWPFRun r : runs) {
                    String text = r.getText(0);
                    text = replaceTag(text, "FullNameNominative", contractorName);
                    r.setText(text, 0);
                }
            }
        }
    }

    private void fillFieldsForIndividualEntrepreneur(XWPFDocument copy, String contractorName) {
        for (XWPFParagraph p : copy.getParagraphs()) {
            List<XWPFRun> runs = p.getRuns();
            if (runs != null) {
                for (XWPFRun r : runs) {
                    String text = r.getText(0);
                    text = replaceTag(text, "FullNameNominative", contractorName);
                    text = replaceTag(text, "RegistrationNumber", contractor.getRegistrationNumber());
                    r.setText(text, 0);
                }
            }
        }
    }

    private void fillFieldsForSelfEmployed(XWPFDocument copy, String contractorName) {
        for (XWPFParagraph p : copy.getParagraphs()) {
            List<XWPFRun> runs = p.getRuns();
            if (runs != null) {
                for (XWPFRun r : runs) {
                    String text = r.getText(0);
                    text = replaceTag(text, "FullNameNominative", contractorName);
                    text = replaceTag(text, "CertificateNumber", contractor.getRegistrationCertificateNumber());
                    text = replaceTag(text, "SelfEmployedDay",
                            WordsConverter.getDayOfMonth(contractor.getSelfemployedDate().getDayOfMonth()));
                    text = replaceTag(text, "SelfEmployedMonth",
                            WordsConverter.convertGenetiveNumericMonthToString(
                                    contractor.getSelfemployedDate().getMonthValue()));
                    text = replaceTag(text, "SelfEmployedYear",
                            "" + contractor.getSelfemployedDate().getYear());
                    text = replaceTag(text, "CertificateDay",
                            WordsConverter.getDayOfMonth(contractor.getRegistrationCertificateDate().getDayOfMonth()));
                    text = replaceTag(text, "CertificateMonth",
                            WordsConverter.convertGenetiveNumericMonthToString(
                                    contractor.getRegistrationCertificateDate().getMonthValue()));
                    text = replaceTag(text, "CertificateYear",
                            "" + contractor.getRegistrationCertificateDate().getYear());
                    r.setText(text, 0);
                }
            }
        }
    }

    private void fillFieldsForLegalEntity(XWPFDocument copy) {
        for (XWPFParagraph p : copy.getParagraphs()) {
            List<XWPFRun> runs = p.getRuns();
            if (runs != null) {
                for (XWPFRun r : runs) {
                    String text = r.getText(0);
                    text = replaceTag(text, "OOOForm", contractor.getOOOForm());
                    text = replaceTag(text, "OOOName", "«" + contractor.getOOOName() + "»");
                    text = replaceTag(text, "PositionGenitive",
                            WordsConverter.convertPositionToGenitive(contractor.getSignatoryPosition()));
                    text = replaceTag(text, "FullNameGenitive",
                            WordsConverter.convertNameToGenitive(contractor.getLastName(),
                                    contractor.getFirstName(), contractor.getSecondName()));
                    text = replaceTag(text, "DocumentType",
                            contractor.getSignatoryPosition().equals("Генеральный директор") ? "Устава"
                                    : "Доверенности №" + contractor.getProxyNumber() +
                                    " от «" + WordsConverter.getDayOfMonth(contractor.getProxyDate().getDayOfMonth()) + "» " +
                                    WordsConverter.convertGenetiveNumericMonthToString(
                                            contractor.getProxyDate().getMonthValue()) + " " +
                                    contractor.getProxyDate().getYear() + "г.");
                    text = replaceTag(text, "VAT", contractor.getTaxPercentage().equals("0")
                            ? "НДС не облагается на основании статьи 346.11 главы 26.2 Налогового кодекса РФ"
                            : "в том числе НДС " + contractor.getTaxPercentage() + "%");
                    r.setText(text, 0);
                }
            }
        }

        for (XWPFTable tbl : copy.getTables()) {
            for (XWPFTableRow row : tbl.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        for (XWPFRun r : p.getRuns()) {
                            String text = r.getText(0);
                            text = replaceTag(text, "SignatoryPosition", contractor.getSignatoryPosition());
                            text = replaceTag(text, "ContractorType", getOOOFromAbbreviation());
                            text = replaceTag(text, "OOOName", "«" + contractor.getOOOName() + "»");
                            r.setText(text, 0);
                        }
                    }
                }
            }
        }
    }

    private void insertRowsInTables(XWPFDocument doc, int tableNumber, int rowsNumber,
                                    List<List<String>> partnerCourses) throws XmlException, IOException {
        XWPFTable table = doc.getTableArray(tableNumber);

        XWPFTableRow oldRow = table.getRow(1);
        CTRow ctrow = CTRow.Factory.parse(oldRow.getCtRow().newInputStream());
        XWPFTableRow newRow = new XWPFTableRow(ctrow, table);

        for (int i = 1, k = 2; i < rowsNumber; i++, k++) {
            List<XWPFTableCell> cells = newRow.getTableCells();
            for (int j = 0; j < cells.size(); j++) {
                for (XWPFParagraph paragraph : cells.get(j).getParagraphs()) {
                    paragraph.getRuns().get(0).setText(partnerCourses.get(i).get(j), 0);
                }
            }
            table.addRow(newRow, k);
        }
    }

    private String getOOOFromAbbreviation() {
        List<String> splitted = Arrays.asList(contractor.getOOOForm().split(" "));
        StringBuilder result = new StringBuilder();
        for (String s : splitted)
            if (s.length() > 1)
                result.append(s.toUpperCase(Locale.ROOT).charAt(0));
        return result.toString();
    }

    private String getCorrectReward(double F) {
        if (report.getReportModel().equals("Чистая выручка")) {
            double reward = BigDecimal.valueOf(F * Double.parseDouble(report.getRoyaltyPercentage()) / 100)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue();
            rewardResult = df.format(reward);
        }
        double rewardResDouble = Double.parseDouble(rewardResult);
        return nf.format(rewardResDouble);
    }

    private int getRubles() {
        return Integer.parseInt(rewardResult.split("\\.")[0]);
    }

    private static String replaceTag(String text, String tag, String replacementText) {
        if (text != null && text.contains(tag))
            text = text.replace(tag, replacementText);
        return text;
    }
}