package org.jabref.gui.entryeditor;

import com.tobiasdiez.easybind.EasyBind;
import javafx.geometry.HPos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import org.controlsfx.control.HyperlinkLabel;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;
import java.net.URLEncoder;

public class SciteTab extends EntryEditorTab {

    public static final String NAME = "Scite";

    private final GridPane searchPane;
    private final ProgressIndicator progressIndicator;
    private final SciteTabViewModel viewModel;
    private final PreferencesService preferencesService;

    public SciteTab(PreferencesService preferencesService, TaskExecutor taskExecutor) {
        this.preferencesService = preferencesService;
        this.viewModel = new SciteTabViewModel(preferencesService, taskExecutor);
        this.searchPane = new GridPane();

        this.progressIndicator = new ProgressIndicator();

        setText(Localization.lang("Scite"));
        setTooltip(new Tooltip(Localization.lang("Search scite.ai for Smart Citations")));
        setSearchPane();
    }

    private void setSearchPane() {
        progressIndicator.setMaxSize(100, 100);
        searchPane.add(progressIndicator, 0, 0);

        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(100);
        column.setHalignment(HPos.CENTER);

        searchPane.getColumnConstraints().setAll(column);
        searchPane.setId("scitePane");
        setContent(searchPane);

        EasyBind.subscribe(viewModel.statusProperty(), status -> {
            searchPane.getChildren().clear();
            switch (status) {
                case IN_PROGRESS:
                    searchPane.add(progressIndicator, 0, 0);
                    break;
                case FOUND:
                    if (viewModel.getCurrentResult().isPresent()) {
                        searchPane.add(getTalliesPane(viewModel.getCurrentResult().get()), 0, 0);
                    }
                    break;
                case ERROR:
                    searchPane.add(getErrorPane(), 0, 0);
                    break;
            }

        });

    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return viewModel.shouldShow();
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(entry);
    }

    private VBox getErrorPane() {
        Label titleLabel = new Label(Localization.lang("Error"));
        titleLabel.setStyle("-fx-font-size: 1.5em;-fx-font-weight: bold;-fx-text-fill: -fx-accent;");
        Text errorMessageText = new Text(viewModel.searchErrorProperty().get());
        VBox errorMessageBox = new VBox(30, titleLabel, errorMessageText);
        errorMessageBox.setStyle("-fx-padding: 30 0 0 30;");
        return errorMessageBox;
    }

    private VBox getTalliesPane(SciteTabViewModel.SciteTallyDTO tallyDTO) {
        Label titleLabel = new Label(Localization.lang("Tallies for " + tallyDTO.getDoi()));
        titleLabel.setStyle("-fx-font-size: 1.5em;-fx-font-weight: bold;");
        Text message = new Text(String.format("Total Citations: %d\nSupporting: %d\nContradicting: %d\nMentioning: %d\nUnclassified: %d\nCiting Publications: %d",
            tallyDTO.getTotal(),
            tallyDTO.getSupporting(),
            tallyDTO.getContradicting(),
            tallyDTO.getMentioning(),
            tallyDTO.getUnclassified(),
            tallyDTO.getCitingPublications()
        ));

        String url = "https://scite.ai/reports/" + URLEncoder.encode(tallyDTO.getDoi());
        HyperlinkLabel link = new HyperlinkLabel(String.format("See full report at [%s]", url));
        link.setOnAction((event) -> {
            if (event.getSource() instanceof Hyperlink) {
                var filePreferences = preferencesService.getFilePreferences();
                try {
                    JabRefDesktop.openBrowser(url, filePreferences);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        VBox messageBox = new VBox(30, titleLabel, message, link);
        messageBox.setStyle("-fx-padding: 30 0 0 30;");
        return messageBox;
    }

}
