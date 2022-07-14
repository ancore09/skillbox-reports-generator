package com.example.application.views.listofreports;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.example.application.data.entity.SampleContractor;
import com.example.application.database.DBManager;
import com.example.application.data.entity.SampleReport;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.*;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import com.example.application.views.MainLayout;

import javax.annotation.security.RolesAllowed;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.dependency.Uses;

@PageTitle("Список отчётов")
@Route(value = "list-of-reports/:sampleReportID?/:action?(edit)", layout = MainLayout.class)
@RolesAllowed("admin")
@Uses(Icon.class)
public class ListofReportsView extends Div {

    private final Grid<SampleReport> grid = new Grid<>(SampleReport.class, false);

    private List<SampleReport> reports = DBManager.getRowsFromReportsTable();
    List<SampleContractor> contractors = DBManager.getRowsFromContractorsTable();

    public ListofReportsView() {
        addClassNames("listof-reports-view", "flex", "flex-col", "h-full");

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();

        createGridLayout(splitLayout);

        add(splitLayout);
        configureGrid();
    }

    private void configureGrid() {
        Editor<SampleReport> editor = grid.getEditor();

        Grid.Column<SampleReport> deleteColumn = grid.addColumn(
                new ComponentRenderer<>(Button::new, (button, report) -> {
                    button.addThemeVariants(ButtonVariant.LUMO_ICON,
                            ButtonVariant.LUMO_ERROR,
                            ButtonVariant.LUMO_TERTIARY);
                    button.addClickListener(e -> this.removeReport(report));
                    button.setIcon(new Icon(VaadinIcon.TRASH));
                })).setFrozen(true).setWidth("150px").setFlexGrow(0);

        Grid.Column<SampleReport> editColumn = grid.addComponentColumn(report -> {
            Button editButton = new Button("Edit");
            editButton.addClickListener(e -> {
                if (editor.isOpen())
                    editor.cancel();
                grid.getEditor().editItem(report);
            });
            return editButton;
        }).setFrozen(true).setWidth("150px").setFlexGrow(0);

        Grid.Column<SampleReport> courseCodeColumn = grid.addColumn("courseCode").setHeader("Номер курса").setAutoWidth(true);
        Grid.Column<SampleReport> courseNameColumn = grid.addColumn("courseName").setHeader("Название курса").setWidth("250px").setResizable(true);
        Grid.Column<SampleReport> courseDirectionColumn = grid.addColumn("courseDirection").setHeader("Направление курса").setWidth("250px").setResizable(true);
        Grid.Column<SampleReport> contractorColumn = grid.addComponentColumn(report -> {
            Accordion contractorAccordion = new Accordion();
            contractorAccordion.close();

            Set<SampleContractor> contractors = DBManager.getContractorsWithReportId(report.getID());
            VerticalLayout contractorsList = new VerticalLayout();
            for (SampleContractor contractor : contractors) {
                contractorsList.add(new Span(contractor.getLastName() + " " + contractor.getFirstName() + " " + contractor.getSecondName()));
            }

            contractorAccordion.add("Контрагент", contractorsList);
            return contractorAccordion;
        }).setHeader("Контрагент").setWidth("250px").setResizable(true);
        Grid.Column<SampleReport> courseObjectColumn = grid.addColumn("courseObject").setHeader("Предмет договора").setWidth("250px").setResizable(true);
        Grid.Column<SampleReport> royaltyColumn = grid.addColumn("royaltyPercentage").setHeader("Ставка по роялти").setAutoWidth(true);
        Grid.Column<SampleReport> contractNumberColumn = grid.addColumn("contractNumber").setHeader("Номер договора").setAutoWidth(true);
        Grid.Column<SampleReport> contractDateColumn = grid.addColumn("contractDate").setHeader("Дата договора").setAutoWidth(true);
        Grid.Column<SampleReport> transferColumn = grid.addColumn("transferDateOfRIA").setHeader("Дата передачи РИД").setAutoWidth(true);
        Grid.Column<SampleReport> k2Column = grid.addColumn("k2").setHeader("К2").setAutoWidth(true);
        Grid.Column<SampleReport> signedEDOColumn = grid.addComponentColumn(report -> {
            Span span = new Span();
            span.setText(report.isSignedEdo() ? "Да" : "Нет");
            return span;
        }).setHeader("Подписание в ЭДО").setAutoWidth(true);

        // NEW //
        configureEditor(editor,
                editColumn,
                courseCodeColumn,
                courseNameColumn,
                courseDirectionColumn,
                contractorColumn,
                courseObjectColumn,
                royaltyColumn,
                contractNumberColumn,
                contractDateColumn,
                transferColumn, k2Column, signedEDOColumn);

        grid.setItems(reports);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);
        grid.setHeightFull();
    }

    // NEW //
    private void configureEditor(Editor<SampleReport> editor, Grid.Column<SampleReport> editColumn, Grid.Column<SampleReport> courseCodeColumn, Grid.Column<SampleReport> courseNameColumn, Grid.Column<SampleReport> courseDirectionColumn, Grid.Column<SampleReport> contractorColumn, Grid.Column<SampleReport> courseObjectColumn, Grid.Column<SampleReport> royaltyColumn, Grid.Column<SampleReport> contractNumberColumn, Grid.Column<SampleReport> contractDateColumn, Grid.Column<SampleReport> transferColumn, Grid.Column<SampleReport> k2Column, Grid.Column<SampleReport> signedEdoColumn) {
        Binder<SampleReport> binder = new Binder<>(SampleReport.class);
        editor.setBinder(binder);
        editor.setBuffered(true);

        TextField courseNumberField = new TextField();
        courseNumberField.setWidthFull();
        binder.forField(courseNumberField)
                .asRequired("Course number must not be empty")
                .bind(SampleReport::getCourseCode, SampleReport::setCourseCode);
        courseCodeColumn.setEditorComponent(courseNumberField);

        TextField courseDirectionField = new TextField();
        courseDirectionField.setWidthFull();
        binder.forField(courseDirectionField)
                .asRequired("Course direction must not be empty")
                //.withValidator(s -> s.equals(editor.getItem().getCourseName()), "Bad")
                .bind(SampleReport::getCourseDirection, SampleReport::setCourseDirection);
        courseDirectionColumn.setEditorComponent(courseDirectionField);

        TextField courseNameField = new TextField();
        courseNameField.setWidthFull();
        binder.forField(courseNameField)
                .asRequired("Course name must not be empty")
                //.withValidator(s -> s.equals(editor.getItem().getCourseName()), "Bad")
                .bind(SampleReport::getCourseName, SampleReport::setCourseName);
        courseNameColumn.setEditorComponent(courseNameField);

        Accordion contractorAccordion = new Accordion();
        contractorAccordion.close();
        CheckboxGroup<SampleContractor> checkboxGroup = new CheckboxGroup<>();
        checkboxGroup.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
        checkboxGroup.setItemLabelGenerator(contractor -> contractor.getLastName() + " " + contractor.getFirstName() + " " + contractor.getSecondName());
        checkboxGroup.setItems(contractors);
        contractorAccordion.add("Контрагент", checkboxGroup);
        binder.forField(checkboxGroup).bind(this::getMultipleContractors, this::setMultipleContractors);
        contractorColumn.setEditorComponent(contractorAccordion);

        TextField courseObjectField = new TextField();
        courseObjectField.setWidthFull();
        binder.forField(courseObjectField)
                .asRequired("Course object must not be empty")
                //.withValidator(s -> s.equals(editor.getItem().getCourseName()), "Bad")
                .bind(SampleReport::getCourseObject, SampleReport::setCourseObject);
        courseObjectColumn.setEditorComponent(courseObjectField);

        TextField royaltyField = new TextField();
        royaltyField.setWidthFull();
        binder.forField(royaltyField)
                .asRequired("Royalty must not be empty")
                //.withValidator(s -> s.equals(editor.getItem().getCourseName()), "Bad")
                .bind(SampleReport::getRoyaltyPercentage, SampleReport::setRoyaltyPercentage);
        royaltyColumn.setEditorComponent(royaltyField);

        TextField contractNumberField = new TextField();
        contractNumberField.setWidthFull();
        binder.forField(contractNumberField)
                .asRequired("Contract number must not be empty")
                //.withValidator(s -> s.equals(editor.getItem().getCourseName()), "Bad")
                .bind(SampleReport::getContractNumber, SampleReport::setContractNumber);
        contractNumberColumn.setEditorComponent(contractNumberField);

        DatePicker contractDatePicker = new DatePicker();
        contractDatePicker.setWidthFull();
        binder.forField(contractDatePicker)
                .asRequired("Contract date must not be empty")
                //.withValidator(s -> s.equals(editor.getItem().getCourseName()), "Bad")
                .bind(SampleReport::getContractDate, SampleReport::setContractDate);
        contractDateColumn.setEditorComponent(contractDatePicker);

        DatePicker transferDatePicker = new DatePicker();
        transferDatePicker.setWidthFull();
        binder.forField(transferDatePicker)
                .asRequired("Transfer date must not be empty")
                //.withValidator(s -> s.equals(editor.getItem().getCourseName()), "Bad")
                .bind(SampleReport::getTransferDateOfRIA, SampleReport::setTransferDateOfRIA);
        transferColumn.setEditorComponent(transferDatePicker);

        TextField k2Field = new TextField();
        k2Field.setWidthFull();
        binder.forField(k2Field)
                .asRequired("K2 must not be empty")
                //.withValidator(s -> s.equals(editor.getItem().getCourseName()), "Bad")
                .bind(SampleReport::getK2, SampleReport::setK2);
        k2Column.setEditorComponent(k2Field);

        Checkbox signedCheckbox = new Checkbox();
        signedCheckbox.setWidthFull();
        binder.forField(signedCheckbox).bind(SampleReport::isSignedEdo, SampleReport::setSignedEdo);
        signedEdoColumn.setEditorComponent(signedCheckbox);


        editor.addSaveListener(editorSaveEvent -> {
            // System.out.println("Report: " + editorSaveEvent.getItem().getCourseName() + " Size of set: " + checkboxGroup.getSelectedItems().size());
            // System.out.println("Edited: " + editorSaveEvent.getItem().getCourseCode());
            DBManager.updateReport(editor.getItem(), checkboxGroup.getSelectedItems());
        });


        Button saveButton = new Button("Save", e -> editor.save());
        Button cancelButton = new Button(VaadinIcon.CLOSE.create(),
                e -> editor.cancel());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_ICON,
                ButtonVariant.LUMO_ERROR);
        HorizontalLayout actions = new HorizontalLayout(saveButton,
                cancelButton);
        actions.setPadding(false);
        editColumn.setEditorComponent(actions);
    }

    private void setMultipleContractors(SampleReport report, Set<SampleContractor> sampleContractors) {
        //System.out.println("Report: " + report.getCourseName() + " Size of set: " + sampleContractors.size());
    }

    private Set<SampleContractor> getMultipleContractors(SampleReport report) {
        return DBManager.getContractorsWithReportId(report.getID());
    }

    private void removeReport(SampleReport report) {
        if (report == null)
            return;
        DBManager.deleteRowFromReports(report.getID());
        reports = DBManager.getRowsFromReportsTable();
        this.refreshGrid();
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setId("grid-wrapper");
        wrapper.setWidthFull();
        splitLayout.addToPrimary(wrapper);
        wrapper.add(grid);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.setItems(reports);
        grid.getDataProvider().refreshAll();
    }
}
