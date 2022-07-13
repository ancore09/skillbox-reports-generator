package com.example.application.database;

import com.example.application.data.entity.SampleContractor;
import com.example.application.data.entity.SampleReport;

import com.vaadin.flow.data.binder.Binder;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class DBManager {

    private static Connection connection;
    private static boolean isConnected = false;

    public static boolean getConnectionState() {
        return isConnected;
    }

    public static void connectDB() {

        try {
            // TODO Change credentials
            connection = DriverManager
                    .getConnection("jdbc:postgresql://172.18.0.2:5432/skillboxreports_new",
                            "postgres", "outofstyle");

            isConnected = true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Opened database successfully");
    }


    /*------------------------------------------------------------*/
    /* USERS TABLE */
    public static List<List<String>> getRowsFromUsersTable() {
        ResultSet rs;
        List<List<String>> users = new ArrayList<>();
        try {
            Statement statement = connection.createStatement();
            String sql = "SELECT * FROM public.users";
            rs = statement.executeQuery(sql);

            while (rs.next()) {
                List<String> user = new ArrayList<>();
                for (int i = 2; i < 5; i++)
                    user.add(rs.getString(i));
                users.add(user);
            }
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        return users;
    }


    /*------------------------------------------------------------*/
    /* REPORTS TABLE */

    public static void createReportsTable() {
        String sql = "CREATE TABLE IF NOT EXISTS public.reports\n" +
                "(\n" +
                "    id integer NOT NULL GENERATED ALWAYS AS IDENTITY ( INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 2147483647 CACHE 1 ),\n" +
                "    report_model text COLLATE pg_catalog.\"default\" NOT NULL,\n" +
                "    course_code integer NOT NULL,\n" +
                "    course_name text COLLATE pg_catalog.\"default\" NOT NULL,\n" +
                "    course_direction text COLLATE pg_catalog.\"default\" NOT NULL,\n" +
                "    contractors text COLLATE pg_catalog.\"default\" NOT NULL,\n" +
                "    royalty_percentage real NOT NULL,\n" +
                "    course_objects text COLLATE pg_catalog.\"default\" NOT NULL,\n" +
                "    contract_number text COLLATE pg_catalog.\"default\" NOT NULL,\n" +
                "    contract_date date NOT NULL,\n" +
                "    transfer_date_of_ria date NOT NULL,\n" +
                "    k2 real,\n" +
                "    CONSTRAINT reports_pkey PRIMARY KEY (id)\n" +
                ")";
        executeStatement(sql, "Reports table is created");
    }

    public static void insertRowIntoReports(Binder<SampleReport> binder) {
        String sql = "INSERT INTO public.reports(\n" +
                "\treport_model, course_code, course_name, course_direction, contractors, royalty_percentage, " +
                "course_objects, contract_number, contract_date, transfer_date_of_ria, k2)\n" +
                "\tVALUES ('" + binder.getBean().getReportModel() + "'," +
                binder.getBean().getCourseCode() + ",'" +
                binder.getBean().getCourseName() + "','" +
                binder.getBean().getCourseDirection() + "','" +
                binder.getBean().getContractor() + "'," +
                binder.getBean().getRoyaltyPercentage() + ",'" +
                binder.getBean().getCourseObject() + "','" +
                binder.getBean().getContractNumber() + "','" +
                binder.getBean().getContractDate() + "','" +
                binder.getBean().getTransferDateOfRIA() + "'," +
                (binder.getBean().getK2().equals("") ? null : binder.getBean().getK2()) + ");";
        executeStatement(sql, "Reports: Row is inserted");
    }

    // NEW //
    public static void insertReport(SampleReport report, Set<SampleContractor> contractors) {
        String insertReportQuery =
                "insert into public.reports (report_model, course_code, course_name, course_direction, royalty_percentage, " +
                        "course_objects, contract_number, contract_date, transfer_date_of_ria, k2) " +
                        "values ('" + report.getReportModel() + "'," +
                        report.getCourseCode() + ",'" +
                        report.getCourseName() + "','" +
                        report.getCourseDirection() + "', " +
                        report.getRoyaltyPercentage() + ",'" +
                        report.getCourseObject() + "','" +
                        report.getContractNumber() + "','" +
                        report.getContractDate() + "','" +
                        report.getTransferDateOfRIA() + "'," +
                        (report.getK2().equals("") ? null : report.getK2()) + ") returning id";
        ResultSet rs;
        try {
            Statement statement = connection.createStatement();
            rs = statement.executeQuery(insertReportQuery);

            if (rs.next()) {
                int id = rs.getInt(1);

                StringBuilder insertQuery = new StringBuilder("insert into public.contractors_reports (contractor_id, report_id) values ");
                for (SampleContractor contractor : contractors) {
                    insertQuery.append("(").append(contractor.getID()).append(", ").append(id).append("),");
                }
                insertQuery.deleteCharAt(insertQuery.length()-1);

                executeStatement(insertQuery.toString(), "CR: Rows are inserted");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }

    public static List<SampleReport> getRowsFromReportsTable() {
        ResultSet rs;
        List<SampleReport> reports = new ArrayList<>();
        try {
            Statement statement = connection.createStatement();
            String sql = "SELECT * FROM public.reports";
            rs = statement.executeQuery(sql);

            while (rs.next()) {
                SampleReport report = new SampleReport();
                report.setId(rs.getInt(1));
                report.setReportModel(rs.getString(2));
                report.setCourseCode("" + rs.getInt(3));
                report.setCourseName(rs.getString(4));
                report.setCourseDirection(rs.getString(5));

                // report.setContractor(rs.getString(6));

                report.setRoyaltyPercentage("" + rs.getDouble(6));
                report.setCourseObject(rs.getString(7));
                report.setContractNumber(rs.getString(8));
                report.setContractDate(rs.getDate(9).toLocalDate());
                report.setTransferDateOfRIA(rs.getDate(10).toLocalDate());
                report.setK2("" + rs.getDouble(11));

                reports.add(report);
            }
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        return reports;
    }

    // NEW //
    public static List<String> getCoursesForContractor(int contractorId) {
        String query = "select distinct r.course_code, r.course_name from reports as r " +
                "left outer join contractors_reports as cr on r.id = cr.report_id " +
                "left outer join contractors as c on cr.contractor_id = c.id where c.id = " + contractorId + " order by r.course_name";
        ResultSet rs;
        List<String> courses = new ArrayList<>();
        try {
            Statement statement = connection.createStatement();
            rs = statement.executeQuery(query);

            while (rs.next()) {
                courses.add("("+rs.getString(1)+") " + rs.getString(2));
            }
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        return courses;
    }

    // NEW //
    public static List<String> getCourses() {
        String query = "select distinct r.course_code, r.course_name from reports as r " +
                "left outer join contractors_reports as cr on r.id = cr.report_id " +
                "left outer join contractors as c on cr.contractor_id = c.id order by r.course_name";
        ResultSet rs;
        List<String> courses = new ArrayList<>();
        try {
            Statement statement = connection.createStatement();
            rs = statement.executeQuery(query);

            while (rs.next()) {
                courses.add("("+rs.getString(1)+") " + rs.getString(2));
            }
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        return courses;
    }

    // NEW //
    public static void updateReport(SampleReport report, Set<SampleContractor> contractors) {
        String updateReportQuery =
                "update public.reports set report_model = '" + report.getReportModel() + "', " +
                "course_code = " + report.getCourseCode() + ", " +
                "course_name = '" + report.getCourseName() + "', " +
                "course_direction = '" + report.getCourseDirection() + "', " +
                "royalty_percentage = '" + report.getRoyaltyPercentage() + "', " +
                "course_objects = '" + report.getCourseObject() + "', " +
                "contract_number = '" + report.getContractNumber() + "', " +
                "contract_date = '" + report.getContractDate() + "', " +
                "transfer_date_of_ria = '" + report.getTransferDateOfRIA() + "', " +
                "k2 = '" + report.getK2() + "'" + "where id = " + report.getID();
        executeStatement(updateReportQuery, "Report: Row is updated");

        String deleteQuery = "delete from public.contractors_reports where report_id = " + report.getID();
        executeStatement(deleteQuery, "Contractors: Rows are deleted");

        StringBuilder insertQuery = new StringBuilder("insert into public.contractors_reports (contractor_id, report_id) values ");
        for (SampleContractor contractor : contractors) {
            insertQuery.append("(").append(contractor.getID()).append(", ").append(report.getID()).append("),");
        }
        insertQuery.deleteCharAt(insertQuery.length()-1);

        executeStatement(insertQuery.toString(), "CR: Rows are inserted");
    }

    public static void deleteRowFromReports(int id) {
        String sql = "DELETE FROM public.reports\n" +
                "\tWHERE id = " + id + ";";
        executeStatement(sql, "Reports: Row is deleted");
    }

    /*------------------------------------------------------------*/
    /* CONTRACTORS TABLE */

    public static void createContractorsTable() {
        String sql = "CREATE TABLE IF NOT EXISTS public.contractors\n" +
                "(\n" +
                "    last_name text COLLATE pg_catalog.\"default\" NOT NULL,\n" +
                "    first_name text COLLATE pg_catalog.\"default\" NOT NULL,\n" +
                "    second_name text COLLATE pg_catalog.\"default\" NOT NULL,\n" +
                "    contractors_type text COLLATE pg_catalog.\"default\" NOT NULL,\n" +
                "    ooo_form text COLLATE pg_catalog.\"default\",\n" +
                "    ooo_name text COLLATE pg_catalog.\"default\",\n" +
                "    tax_percentage integer,\n" +
                "    signatory_position text COLLATE pg_catalog.\"default\",\n" +
                "    selfemployed_date date,\n" +
                "    registration_certificate_number text COLLATE pg_catalog.\"default\",\n" +
                "    registration_certificate_date date,\n" +
                "    registration_number text COLLATE pg_catalog.\"default\",\n" +
                "    itn text COLLATE pg_catalog.\"default\",\n" +
                "    proxy_number text COLLATE pg_catalog.\"default\",\n" +
                "    proxy_date date,\n" +
                "    id integer NOT NULL GENERATED ALWAYS AS IDENTITY ( INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 2147483647 CACHE 1 ),\n" +
                "    CONSTRAINT contractors_pkey PRIMARY KEY (id)\n" +
                ")";
        executeStatement(sql, "Contractors table is created");
    }

    public static void insertRowIntoContractors(Binder<SampleContractor> binder) {
        String OOOForm = getStringForTable(binder.getBean().getOOOForm());
        String OOOName = getStringForTable(binder.getBean().getOOOName());
        String taxPercentage = getStringForTable(binder.getBean().getTaxPercentage());
        String signatoryPosition = getStringForTable(binder.getBean().getSignatoryPosition());
        String selfemployedDate = getStringForTable(binder.getBean().getSelfemployedDate());
        String registrationCertificateNumber = getStringForTable(binder.getBean().getRegistrationCertificateNumber());
        String registrationCertificateDate = getStringForTable(binder.getBean().getRegistrationCertificateDate());
        String registrationNumber = getStringForTable(binder.getBean().getRegistrationNumber());
        String ITN = getStringForTable(binder.getBean().getITN());
        String proxyNumber = getStringForTable(binder.getBean().getProxyNumber());
        String proxyDate = getStringForTable(binder.getBean().getProxyDate());

        if (taxPercentage != null)
            taxPercentage = taxPercentage.replaceAll("'", "");

        String sql = "INSERT INTO public.contractors(\n" +
                "\tlast_name, first_name, second_name, contractors_type, " +
                "ooo_form, ooo_name, tax_percentage, signatory_position, selfemployed_date, " +
                "registration_certificate_number, registration_certificate_date, " +
                "registration_number, itn, proxy_number, proxy_date)\n" +
                "\tVALUES ('" +
                binder.getBean().getLastName() + "','" +
                binder.getBean().getFirstName() + "','" +
                binder.getBean().getSecondName() + "','" +
                binder.getBean().getContractorType() + "'," +
                OOOForm + "," + OOOName + "," + taxPercentage + "," +
                signatoryPosition + "," + selfemployedDate + "," + registrationCertificateNumber + "," +
                registrationCertificateDate + "," + registrationNumber +
                "," + ITN + "," + proxyNumber + "," + proxyDate + ");";
        executeStatement(sql, "Contractors: Row is inserted");
    }

    private static String getStringForTable(Object obj) {
        if ((obj instanceof String && !obj.equals("")) || obj instanceof LocalDate)
            return "'" + obj + "'";
        else
            return null;
    }

    public static List<SampleContractor> getRowsFromContractorsTable() {
        ResultSet rs;
        List<SampleContractor> contractors = new ArrayList<>();
        try {
            Statement statement = connection.createStatement();
            String sql = "SELECT * FROM public.contractors";
            rs = statement.executeQuery(sql);

            while (rs.next()) {
                SampleContractor contractor = new SampleContractor();
//                contractor.setLastName(rs.getString(1));
//                contractor.setFirstName(rs.getString(2));
//                contractor.setSecondName(rs.getString(3));
//                contractor.setContractorType(rs.getString(4));
//                contractor.setOOOForm(rs.getObject(5) != null
//                        ? rs.getString(5) : null);
//                contractor.setOOOName(rs.getObject(6) != null
//                        ? rs.getString(6) : null);
//                contractor.setTaxPercentage(rs.getObject(7) != null
//                        ? "" + rs.getInt(7) : null);
//                contractor.setSignatoryPosition(rs.getObject(8) != null
//                        ? rs.getString(8) : null);
//                contractor.setSelfemployedDate(rs.getObject(9) != null
//                        ? rs.getDate(9).toLocalDate() : null);
//                contractor.setRegistrationCertificateNumber(rs.getObject(10) != null
//                        ? rs.getString(10) : null);
//                contractor.setRegistrationCertificateDate(rs.getObject(11) != null
//                        ? rs.getDate(11).toLocalDate() : null);
//                contractor.setRegistrationNumber(rs.getObject(12) != null
//                        ? rs.getString(12) : null);
//                contractor.setITN(rs.getObject(13) != null
//                        ? rs.getString(13) : null);
//                contractor.setProxyNumber(rs.getObject(14) != null
//                        ? rs.getString(14) : null);
//                contractor.setProxyDate(rs.getObject(15) != null
//                        ? rs.getDate(15).toLocalDate() : null);
//                contractor.setId(rs.getInt(16));


                // NEW //
                contractor.setId(rs.getInt(1));
                contractor.setLastName(rs.getString(2));
                contractor.setFirstName(rs.getString(3));
                contractor.setSecondName(rs.getString(4));
                contractor.setContractorType(rs.getString(5));
                contractor.setOOOForm(rs.getObject(6) != null
                        ? rs.getString(6) : null);
                contractor.setOOOName(rs.getObject(7) != null
                        ? rs.getString(7) : null);
                contractor.setTaxPercentage(rs.getObject(8) != null
                        ? "" + rs.getInt(8) : null);
                contractor.setSignatoryPosition(rs.getObject(9) != null
                        ? rs.getString(9) : null);
                contractor.setSelfemployedDate(rs.getObject(10) != null
                        ? rs.getDate(10).toLocalDate() : null);
                contractor.setRegistrationCertificateNumber(rs.getObject(11) != null
                        ? rs.getString(11) : null);
                contractor.setRegistrationCertificateDate(rs.getObject(12) != null
                        ? rs.getDate(12).toLocalDate() : null);
                contractor.setRegistrationNumber(rs.getObject(13) != null
                        ? rs.getString(13) : null);
                contractor.setITN(rs.getObject(14) != null
                        ? rs.getString(14) : null);
                contractor.setProxyNumber(rs.getObject(15) != null
                        ? rs.getString(15) : null);
                contractor.setProxyDate(rs.getObject(16) != null
                        ? rs.getDate(16).toLocalDate() : null);

                contractors.add(contractor);
            }
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        return contractors;
    }

    // NEW //
    public static Set<SampleContractor> getContractorsWithReportId(int reportId) {
        ResultSet rs;
        Set<SampleContractor> contractors = new HashSet<>();

        try {
            Statement statement = connection.createStatement();
            String sql = "select * from public.contractors where id in (select contractor_id from contractors_reports where report_id = " + reportId +")";
            rs = statement.executeQuery(sql);

            while (rs.next()) {
                SampleContractor contractor = new SampleContractor();

                contractor.setId(rs.getInt(1));
                contractor.setLastName(rs.getString(2));
                contractor.setFirstName(rs.getString(3));
                contractor.setSecondName(rs.getString(4));
                contractor.setContractorType(rs.getString(5));
                contractor.setOOOForm(rs.getObject(6) != null
                        ? rs.getString(6) : null);
                contractor.setOOOName(rs.getObject(7) != null
                        ? rs.getString(7) : null);
                contractor.setTaxPercentage(rs.getObject(8) != null
                        ? "" + rs.getInt(8) : null);
                contractor.setSignatoryPosition(rs.getObject(9) != null
                        ? rs.getString(9) : null);
                contractor.setSelfemployedDate(rs.getObject(10) != null
                        ? rs.getDate(10).toLocalDate() : null);
                contractor.setRegistrationCertificateNumber(rs.getObject(11) != null
                        ? rs.getString(11) : null);
                contractor.setRegistrationCertificateDate(rs.getObject(12) != null
                        ? rs.getDate(12).toLocalDate() : null);
                contractor.setRegistrationNumber(rs.getObject(13) != null
                        ? rs.getString(13) : null);
                contractor.setITN(rs.getObject(14) != null
                        ? rs.getString(14) : null);
                contractor.setProxyNumber(rs.getObject(15) != null
                        ? rs.getString(15) : null);
                contractor.setProxyDate(rs.getObject(16) != null
                        ? rs.getDate(16).toLocalDate() : null);

                contractors.add(contractor);
            }

            // System.out.println("Id: " + reportId + " - " + contractors.size());

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }

        return contractors;
    }

    // NEW //
    public static void updateContractor(SampleContractor contractor) {
        String tax = getStringForTable(contractor.getTaxPercentage());
        if (tax != null) {
            tax = tax.replace("'", "");
        }
        String updateContractorQuery =
                "update public.contractors set last_name = " + getStringForTable(contractor.getLastName()) + ", " +
                "first_name = " + getStringForTable(contractor.getFirstName()) + ", " +
                "second_name = " + getStringForTable(contractor.getSecondName()) + "," +
                "ooo_form = " + getStringForTable(contractor.getOOOForm()) + ", " +
                "ooo_name = " + getStringForTable(contractor.getOOOName()) + ", " +
                "tax_percentage = " + tax + ", " +
                "signatory_position = " + getStringForTable(contractor.getSignatoryPosition()) + ", " +
                "selfemployed_date = " + getStringForTable(contractor.getSelfemployedDate()) + ", " +
                "registration_certificate_number = " + getStringForTable(contractor.getRegistrationCertificateNumber()) + ", " +
                "registration_certificate_date = " + getStringForTable(contractor.getRegistrationCertificateDate()) + ", " +
                "registration_number = " + getStringForTable(contractor.getRegistrationNumber()) + ", " +
                "itn = " + getStringForTable(contractor.getITN()) + ", " +
                "proxy_number = " + getStringForTable(contractor.getProxyNumber()) + ", " +
                "proxy_date = " + getStringForTable(contractor.getProxyDate()) + " where id = " + contractor.getID();
        executeStatement(updateContractorQuery, "Contractors: Row is updated");
    }

    public static void deleteRowFromContractors(int id) {
        String sql = "DELETE FROM public.contractors\n" +
                "\tWHERE id = " + id + ";";
        executeStatement(sql, "Contractors: Row is deleted");
    }


    /*------------------------------------------------------------*/
    /* LOGS TABLE */

    public static void createLogsTable() {
        String sql = "CREATE TABLE IF NOT EXISTS public.logs\n" +
                "(\n" +
                "    date date NOT NULL,\n" +
                "    login text COLLATE pg_catalog.\"default\" NOT NULL,\n" +
                "    course_code integer NOT NULL,\n" +
                "    contractor text COLLATE pg_catalog.\"default\" NOT NULL,\n" +
                "    period text COLLATE pg_catalog.\"default\" NOT NULL\n" +
                ")";
        executeStatement(sql, "Logs table is created");
    }

    public static void insertRowIntoLogs(LocalDate date, String login, int courseCode,
                                         SampleContractor contractor, String period) {
        String sql = "INSERT INTO public.logs(\n" +
                "\tdate, login, course_code, contractor, period)\n" +
                "\tVALUES ('" + date + "', '" + login + "', " + courseCode + ", '" +
                contractor.getLastName() + " " + contractor.getFirstName() + " " + contractor.getSecondName() + "', '" +
                period + "');";
        executeStatement(sql, "Logs: Row is inserted");
    }

    public static List<String> getRowsFromLogsTable() {
        ResultSet rs;
        List<String> logs = new ArrayList<>();
        try {
            Statement statement = connection.createStatement();
            String sql = "SELECT * FROM public.logs";
            rs = statement.executeQuery(sql);

            while (rs.next()) {
                StringBuilder sb = new StringBuilder();
                sb.append(rs.getDate(1));
                sb.append("\tINFO\t");
                sb.append("[").append(rs.getString(2)).append("]\t:\t");
                sb.append(rs.getInt(3)).append("\t-\t");
                sb.append(rs.getString(4)).append("\t---\t");
                sb.append(rs.getString(5));

                logs.add(sb.toString());
            }
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        return logs;
    }

    public static void deleteRowFromLogs(LocalDate date) {
        String sql = "DELETE FROM public.logs\n" +
                "\tWHERE date <= '" + date.minusMonths(1) + "';";
        executeStatement(sql, "Logs: Row is deleted");
    }

    // NEW //
    public static void createCSVFromLogsTable(String filename) throws SQLException, IOException {
        CopyManager copyManager = new CopyManager((BaseConnection) connection);
        // TODO Change to storage folder
        String path = "/logs/" + filename;
        File file = new File(path);
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        String sql = "SELECT * FROM public.logs";
        copyManager.copyOut("COPY (" + sql + ") TO STDOUT WITH (FORMAT CSV, HEADER)", fileOutputStream);
    }



    /*------------------------------------------------------------*/
    /* COMMON */

    public static void executeStatement(String sql, String message) {
        try {
            Statement statement = connection.createStatement();
            statement.execute(sql);
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println(message);
    }
}
