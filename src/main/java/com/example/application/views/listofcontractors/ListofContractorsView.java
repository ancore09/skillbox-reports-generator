package com.example.application.views.listofcontractors;

import java.util.List;

import com.example.application.data.entity.SampleReport;
import com.example.application.database.DBManager;
import com.example.application.data.entity.SampleContractor;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import com.example.application.views.MainLayout;

import javax.annotation.security.RolesAllowed;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.dependency.Uses;
import org.apache.poi.ss.formula.functions.T;

@PageTitle("Список контрагентов")
@Route(value = "list-of-contactors/:sampleContractorID?/:action?(edit)", layout = MainLayout.class)
@RolesAllowed("admin")
@Uses(Icon.class)
public class ListofContractorsView extends Div {

    private final Grid<SampleContractor> grid = new Grid<>(SampleContractor.class, false);

    private List<SampleContractor> contractors = DBManager.getRowsFromContractorsTable();

    public ListofContractorsView() {
        addClassNames("listof-contractors-view", "flex", "flex-col", "h-full");

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();

        createGridLayout(splitLayout);

        add(splitLayout);
        configureGrid();
    }

    private void configureGrid() {
        Editor<SampleContractor> editor = grid.getEditor();

        Grid.Column<SampleContractor> deleteColumn = grid.addColumn(
                new ComponentRenderer<>(Button::new, (button, contractor) -> {
                    button.addThemeVariants(ButtonVariant.LUMO_ICON,
                            ButtonVariant.LUMO_ERROR,
                            ButtonVariant.LUMO_TERTIARY);
                    button.addClickListener(e -> this.removeContractor(contractor));
                    button.setIcon(new Icon(VaadinIcon.TRASH));
                })).setFrozen(true);

        Grid.Column<SampleContractor> editColumn = grid.addComponentColumn(contractor -> {
            Button editButton = new Button("Edit");
            editButton.addClickListener(e -> {
                if (editor.isOpen())
                    editor.cancel();
                grid.getEditor().editItem(contractor);
            });
            return editButton;
        }).setWidth("150px").setFrozen(true).setFlexGrow(0);

        Grid.Column<SampleContractor> lastNameColumn = grid.addColumn("lastName").setHeader("Фамилия").setAutoWidth(true);
        Grid.Column<SampleContractor> firstNameColumn = grid.addColumn("firstName").setHeader("Имя").setAutoWidth(true);
        Grid.Column<SampleContractor> secondNameColumn = grid.addColumn("secondName").setHeader("Отчество").setAutoWidth(true);
        Grid.Column<SampleContractor> contractorTypeColumn = grid.addColumn("contractorType").setHeader("Тип контрагента").setAutoWidth(true);
        Grid.Column<SampleContractor> OOOFormColumn = grid.addColumn("OOOForm").setHeader("Форма юридического лица").setAutoWidth(true);
        Grid.Column<SampleContractor> OOONameColumn = grid.addColumn("OOOName").setHeader("Наименование юридического лица").setAutoWidth(true);
        Grid.Column<SampleContractor> taxPercentageColumn = grid.addColumn("taxPercentage").setHeader("Ставка по налогам").setAutoWidth(true);
        Grid.Column<SampleContractor> signatoryPositionColumn = grid.addColumn("signatoryPosition").setHeader("Должность подписанта").setAutoWidth(true);
        Grid.Column<SampleContractor> selfemployedDateColumn = grid.addColumn("selfemployedDate").setHeader("Дата постановки самозанятым").setAutoWidth(true);
        Grid.Column<SampleContractor> registrationCertificateNumberColumn = grid.addColumn("registrationCertificateNumber").setHeader("Номер справки постановки на учет").setAutoWidth(true);
        Grid.Column<SampleContractor> registrationCertificateDateColumn = grid.addColumn("registrationCertificateDate").setHeader("Дата справки постановки на учет").setAutoWidth(true);
        Grid.Column<SampleContractor> registrationNumberColumn = grid.addColumn("registrationNumber").setHeader("ОГРНИП").setAutoWidth(true);
        Grid.Column<SampleContractor> ITNColumn = grid.addColumn("ITN").setHeader("ИНН").setAutoWidth(true);
        Grid.Column<SampleContractor> proxyNumberColumn = grid.addColumn("proxyNumber").setHeader("Номер доверенности").setAutoWidth(true);
        Grid.Column<SampleContractor> proxyDateColumn = grid.addColumn("proxyDate").setHeader("Дата получения доверенности").setAutoWidth(true);

        // NEW //
        configureEditor(editor,
                editColumn,
                lastNameColumn,
                firstNameColumn,
                secondNameColumn,
                OOOFormColumn,
                OOONameColumn,
                taxPercentageColumn,
                signatoryPositionColumn,
                selfemployedDateColumn,
                registrationCertificateNumberColumn,
                registrationCertificateDateColumn,
                registrationNumberColumn,
                ITNColumn,
                proxyNumberColumn, proxyDateColumn);


        grid.setItems(contractors);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);
        grid.setHeightFull();
    }

    // NEW //
    private void configureEditor(Editor<SampleContractor> editor, Grid.Column<SampleContractor> editColumn, Grid.Column<SampleContractor> lastNameColumn, Grid.Column<SampleContractor> firstNameColumn, Grid.Column<SampleContractor> secondNameColumn, Grid.Column<SampleContractor> OOOFormColumn, Grid.Column<SampleContractor> OOONameColumn, Grid.Column<SampleContractor> taxPercentageColumn, Grid.Column<SampleContractor> signatoryPositionColumn, Grid.Column<SampleContractor> selfemployedDateColumn, Grid.Column<SampleContractor> registrationCertificateNumberColumn, Grid.Column<SampleContractor> registrationCertificateDateColumn, Grid.Column<SampleContractor> registrationNumberColumn, Grid.Column<SampleContractor> ITNColumn, Grid.Column<SampleContractor> proxyNumberColumn, Grid.Column<SampleContractor> proxyDateColumn) {
        Binder<SampleContractor> binder = new Binder<>(SampleContractor.class);
        editor.setBinder(binder);
        editor.setBuffered(true);

        TextField lastNameField = new TextField();
        lastNameField.setWidthFull();
        //lastNameField.setVisible(false);
        binder.forField(lastNameField)
                .asRequired("Last name must not be empty")
                .bind(SampleContractor::getLastName, SampleContractor::setLastName);
        lastNameColumn.setEditorComponent(lastNameField);

        TextField firstNameField = new TextField();
        firstNameField.setWidthFull();
        //firstNameField.setVisible(false);
        binder.forField(firstNameField)
                .asRequired("First name must not be empty")
                .bind(SampleContractor::getFirstName, SampleContractor::setFirstName);
        firstNameColumn.setEditorComponent(firstNameField);

        TextField secondNameField = new TextField();
        secondNameField.setWidthFull();
        //secondNameField.setVisible(false);
        binder.forField(secondNameField)
                .asRequired("Second name must not be empty")
                .bind(SampleContractor::getSecondName, SampleContractor::setSecondName);
        secondNameColumn.setEditorComponent(secondNameField);

        TextField OOOFormField = new TextField();
        OOOFormField.setWidthFull();
        OOOFormField.setVisible(false);
        binder.forField(OOOFormField)
                .withValidator(s -> !OOOFormField.isVisible() || OOOFormField.isVisible() && !s.isEmpty(), "Form name must not be empty")
                .bind(SampleContractor::getOOOForm, SampleContractor::setOOOForm);
        OOOFormColumn.setEditorComponent(OOOFormField);

        TextField OOONameField = new TextField();
        OOONameField.setWidthFull();
        OOONameField.setVisible(false);
        binder.forField(OOONameField)
                .withValidator(s -> !OOONameField.isVisible() || OOONameField.isVisible() && !s.isEmpty(), "Form name must not be empty")
                .bind(SampleContractor::getOOOName, SampleContractor::setOOOName);
        OOONameColumn.setEditorComponent(OOONameField);

        TextField taxPercentageField = new TextField();
        taxPercentageField.setWidthFull();
        taxPercentageField.setVisible(false);
        binder.forField(taxPercentageField)
                .withValidator(s -> !taxPercentageField.isVisible() || taxPercentageField.isVisible() && !s.isEmpty(), "Form name must not be empty")
                .bind(SampleContractor::getTaxPercentage, SampleContractor::setTaxPercentage);
        taxPercentageColumn.setEditorComponent(taxPercentageField);

        TextField signatoryPositionField = new TextField();
        signatoryPositionField.setWidthFull();
        signatoryPositionField.setVisible(false);
        binder.forField(signatoryPositionField)
                .withValidator(s -> !signatoryPositionField.isVisible() || signatoryPositionField.isVisible() && !s.isEmpty(), "Form name must not be empty")
                .bind(SampleContractor::getSignatoryPosition, SampleContractor::setSignatoryPosition);
        signatoryPositionColumn.setEditorComponent(signatoryPositionField);

        DatePicker selfemployedDatePicker = new DatePicker();
        selfemployedDatePicker.setWidthFull();
        selfemployedDatePicker.setVisible(false);
        binder.forField(selfemployedDatePicker)
                .withValidator(s -> !selfemployedDatePicker.isVisible() || selfemployedDatePicker.isVisible() && !s.toString().isEmpty(), "Form name must not be empty")
                .bind(SampleContractor::getSelfemployedDate, SampleContractor::setSelfemployedDate);
        selfemployedDateColumn.setEditorComponent(selfemployedDatePicker);

        TextField registrationCertificateNumberField = new TextField();
        registrationCertificateNumberField.setWidthFull();
        registrationCertificateNumberField.setVisible(false);
        binder.forField(registrationCertificateNumberField)
                .withValidator(s -> !registrationCertificateNumberField.isVisible() || registrationCertificateNumberField.isVisible() && !s.isEmpty(), "Form name must not be empty")
                .bind(SampleContractor::getRegistrationCertificateNumber, SampleContractor::setRegistrationCertificateNumber);
        registrationCertificateNumberColumn.setEditorComponent(registrationCertificateNumberField);

        DatePicker registrationCertificateDatePicker = new DatePicker();
        registrationCertificateDatePicker.setWidthFull();
        registrationCertificateDatePicker.setVisible(false);
        binder.forField(registrationCertificateDatePicker)
                .withValidator(s -> !registrationCertificateDatePicker.isVisible() || registrationCertificateDatePicker.isVisible() && !s.toString().isEmpty(), "Form name must not be empty")
                .bind(SampleContractor::getRegistrationCertificateDate, SampleContractor::setRegistrationCertificateDate);
        registrationCertificateDateColumn.setEditorComponent(registrationCertificateDatePicker);

        TextField registrationNumberField = new TextField();
        registrationNumberField.setWidthFull();
        registrationCertificateDatePicker.setVisible(false);
        binder.forField(registrationNumberField)
                .withValidator(s -> !registrationNumberField.isVisible() || registrationNumberField.isVisible() && !s.isEmpty(), "Form name must not be empty")
                .bind(SampleContractor::getRegistrationNumber, SampleContractor::setRegistrationNumber);
        registrationNumberColumn.setEditorComponent(registrationNumberField);

        TextField ITNField = new TextField();
        ITNField.setWidthFull();
        binder.forField(ITNField)
                .withValidator(s -> !ITNField.isVisible() || ITNField.isVisible() && !s.isEmpty(), "Form name must not be empty")
                .bind(SampleContractor::getITN, SampleContractor::setITN);
        ITNColumn.setEditorComponent(ITNField);

        TextField proxyNumberField = new TextField();
        proxyNumberField.setWidthFull();
        proxyNumberField.setVisible(false);
        binder.forField(proxyNumberField)
                .bind(SampleContractor::getProxyNumber, SampleContractor::setProxyNumber);
        proxyNumberColumn.setEditorComponent(proxyNumberField);

        DatePicker proxyDatePicker = new DatePicker();
        proxyDatePicker.setWidthFull();
        proxyDatePicker.setVisible(false);
        binder.forField(proxyDatePicker)
                .bind(SampleContractor::getProxyDate, SampleContractor::setProxyDate);
        proxyDateColumn.setEditorComponent(proxyDatePicker);

        editor.addOpenListener(editorOpenEvent -> {
            SampleContractor contractor = editor.getItem();
            switch (contractor.getContractorType()) {
                case "Юридическое лицо":
                    OOOFormField.setVisible(true);
                    OOONameField.setVisible(true);
                    taxPercentageField.setVisible(true);
                    signatoryPositionField.setVisible(true);
                    ITNField.setVisible(true);
                    proxyDatePicker.setVisible(true);
                    proxyNumberField.setVisible(true);
                    break;
                case "Физическое лицо":
                    ITNField.setVisible(true);
                    break;
                case "Самозанятый":
                    selfemployedDatePicker.setVisible(true);
                    ITNField.setVisible(true);
                    registrationCertificateNumberField.setVisible(true);
                    registrationCertificateDatePicker.setVisible(true);
                    break;
                case "Индивидуальный предприниматель":
                    ITNField.setVisible(true);
                    registrationNumberField.setVisible(true);
                    break;
                default:
                    break;
            }
        });

        editor.addCloseListener(editorCloseEvent -> {
            setVisibilityFalse(
                    OOOFormField,
                    OOONameField,
                    taxPercentageField,
                    signatoryPositionField,
                    selfemployedDatePicker,
                    registrationCertificateNumberField,
                    registrationCertificateDatePicker,
                    registrationNumberField, ITNField, proxyNumberField, proxyDatePicker);
        });

        editor.addCancelListener(editorCancelEvent -> {
            setVisibilityFalse(
                    OOOFormField,
                    OOONameField,
                    taxPercentageField,
                    signatoryPositionField,
                    selfemployedDatePicker,
                    registrationCertificateNumberField,
                    registrationCertificateDatePicker,
                    registrationNumberField, ITNField, proxyNumberField, proxyDatePicker);
        });

        editor.addSaveListener(editorSaveEvent -> {
            setVisibilityFalse(
                    OOOFormField,
                    OOONameField,
                    taxPercentageField,
                    signatoryPositionField,
                    selfemployedDatePicker,
                    registrationCertificateNumberField,
                    registrationCertificateDatePicker,
                    registrationNumberField, ITNField, proxyNumberField, proxyDatePicker);

            DBManager.updateContractor(editorSaveEvent.getItem());
            System.out.println("Edited: " + editorSaveEvent.getItem().getLastName());
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

    private void setVisibilityFalse(TextField OOOFormField, TextField OOONameField, TextField taxPercentageField, TextField signatoryPositionField, DatePicker selfemployedDatePicker, TextField registrationCertificateNumberField, DatePicker registrationCertificateDatePicker, TextField registrationNumberField, TextField ITNField, TextField proxyNumber, DatePicker proxyDate) {
        OOOFormField.setVisible(false);
        OOONameField.setVisible(false);
        taxPercentageField.setVisible(false);
        signatoryPositionField.setVisible(false);
        ITNField.setVisible(false);
        selfemployedDatePicker.setVisible(false);
        registrationCertificateNumberField.setVisible(false);
        registrationCertificateDatePicker.setVisible(false);
        registrationNumberField.setVisible(false);
        proxyDate.setVisible(false);
        proxyNumber.setVisible(false);
    }

    private void removeContractor(SampleContractor contractor) {
        if (contractor == null)
            return;
        DBManager.deleteRowFromContractors(contractor.getID());
        contractors = DBManager.getRowsFromContractorsTable();
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
        grid.setItems(contractors);
        grid.getDataProvider().refreshAll();
    }
}
