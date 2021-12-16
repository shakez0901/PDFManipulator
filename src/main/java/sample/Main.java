package sample;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
// import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import com.sun.javafx.scene.control.skin.Utils;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.Matrix;
import org.imgscalr.Scalr;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


public class Main extends Application {
    private Stage window;

    private TilePane previewPane;
    private TextField scaleTextField, currentPageField;
    private int currentPage;
    private Label totalPagesLabel;
    private TextField pageRangeField; 
    
    // private TextField lastPageField = new TextField();

    private FileChooser fileChooser;
    private File lastPath;
    private String fileName;
    private PDDocument document;
    private BufferedImage preview;

    private boolean isOpen = false;

    private int lowerRange;
    private int upperRange;

    private Label sizeLabel;


    public static void main(String[] args) {
        try {
            launch(args);
        } catch (Exception e) {
            Alert error = new Alert(Alert.AlertType.ERROR, "Something went wrong! \n" + e.getMessage(), ButtonType.OK);
            error.showAndWait();
        }
    }

    @Override
    public void start(Stage primaryStage) {
        fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"), new FileChooser.ExtensionFilter("All Files", "*.*"));
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "/Desktop"));

        window = primaryStage;
        window.setTitle("PDFManipulator");

        Button saveButton = new Button("Save");
        saveButton.setOnAction(e -> saveFile());

        Button openButton = new Button("Open");
        openButton.setOnAction(e -> {
            try {
                openFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        Button prevPageButton = new Button("<");
        prevPageButton.setOnAction(e -> {
             currentPage--;
            if (currentPage < lowerRange) {
                currentPage = upperRange;
            }
            previewImage();
        });

        Button nextPageButton = new Button(">");
        nextPageButton.setOnAction(e -> {
            currentPage++;
            if (currentPage > upperRange) {
                currentPage = lowerRange;
            }
            previewImage();
        });

        Button firstPageButton = new Button("<<");
        firstPageButton.setOnAction(e -> {
            currentPage = lowerRange;
            previewImage();
        });

        Button lastPageButton = new Button(">>");
        lastPageButton.setOnAction(e -> {
            currentPage = upperRange;
            previewImage();
        });

        Button rotate180Button = new Button("Rotate 180°");
        rotate180Button.setOnAction(e -> {

            try {
                rotatePages(180);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        });

        Button rotateLeftButton = new Button("Rotate left");
        rotateLeftButton.setOnAction(e -> {

            try {
                rotatePages(90);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        });

        Button rotateRightButton = new Button("Rotate right");
        rotateRightButton.setOnAction(e -> {

            try {
                rotatePages(-90);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        });


        Button splitMidVertButton = new Button("Split vertically");
        splitMidVertButton.setOnAction(e -> {
            try {
                splitPages("vertically");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });


        Button splitMidHorButton = new Button("Split horizontally");
        splitMidHorButton.setOnAction(e -> {
            try {
                splitPages("horizontally");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });


        Button scaleButton = new Button("Scale");
        scaleButton.setOnAction(e -> {
            scalePDF();
        });
        scaleTextField = new TextField();
        scaleTextField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                scalePDF();
            }
        });


        Button previewButton = new Button("Preview");
        previewButton.setOnAction(e -> {
            String text = currentPageField.getText();
            if (text.equals("")) {
                currentPage = 1;
            } else {
                try {
                    currentPage = Integer.parseInt(text);
                    if (currentPage > document.getNumberOfPages()) {
                        currentPage = document.getNumberOfPages();
                    }
                } catch (IllegalArgumentException e1) {
                    Alert error = new Alert(Alert.AlertType.ERROR, "Invalid Input", ButtonType.OK);
                    error.showAndWait();
                }

            }
            previewImage();
        });
        currentPageField = new TextField();
        currentPageField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                try {
                    currentPage = Integer.parseInt(currentPageField.getText());
                } catch (IllegalArgumentException e1) {
                    Alert error = new Alert(Alert.AlertType.ERROR, "Invalid Input", ButtonType.OK);
                    error.showAndWait();
                }

                if (currentPage < 0 || currentPage > document.getNumberOfPages() - 1) {
                    Alert error = new Alert(Alert.AlertType.ERROR, "Invalid Page Number", ButtonType.OK);
                    error.showAndWait();
                } else {
                    previewImage();
                }

            }
        });
        totalPagesLabel = new Label();
    
        pageRangeField = new TextField();
        pageRangeField.setPromptText("range e.g. 1-5");
        
        pageRangeField.setPrefWidth(pageRangeField.promptTextProperty().getName().length()*10);
        pageRangeField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            try {
                if (!newVal && isOpen){
                    //update values when textfield loses focus
                    String s = pageRangeField.getText();
                    if (!s.equals("")) {
                        String[] split = pageRangeField.getText().split("-"); //range i-j
                        int l = Integer.parseInt(split[0]);
                        int u = Integer.parseInt(split[1]);
                        updateRange(l, u);
                    } else {
                        updateRange(1, document.getNumberOfPages());
                    }
                    previewImage();
                }
            } catch(NumberFormatException | IndexOutOfBoundsException a) {
                Alert error = new Alert(Alert.AlertType.ERROR, "Invalid range", ButtonType.OK);
                error.showAndWait();
                updateRange(1, document.getNumberOfPages());
            }catch (Exception e) {
                //TODO: handle exception
            }
        });
        

        previewPane = new TilePane();
        previewPane.setPrefColumns(1);
        previewPane.setPrefRows(1);
        sizeLabel = new Label();

        VBox layout1 = new VBox(16);

        HBox box1 = new HBox(16);
        box1.getChildren().addAll(saveButton, openButton);

        //layout for navigation
        HBox box2 = new HBox(16);
        box2.getChildren().addAll(firstPageButton, prevPageButton, nextPageButton, lastPageButton);

        //layout for manipulation buttons
        HBox box3 = new HBox(16);
        VBox scaleBox = new VBox();
        scaleBox.getChildren().addAll(scaleButton, scaleTextField);
        box3.getChildren().addAll(scaleBox, rotateLeftButton, rotateRightButton, rotate180Button, splitMidHorButton, splitMidVertButton);

        HBox box4 = new HBox();
        box4.getChildren().addAll(currentPageField, totalPagesLabel,new Separator(), pageRangeField);
        HBox box5 = new HBox();
        box5.getChildren().addAll(previewPane, sizeLabel);

        VBox prevBox = new VBox();
        prevBox.getChildren().addAll(previewButton, box4);

        //add everything to the layout
        layout1.getChildren().addAll(box1, box3, prevBox, box5, box2);


        Scene scene = new Scene(layout1);
        window.setScene(scene);
        window.show();

        //match size of textfield with corresponding button
        scaleTextField.setPrefWidth(scaleButton.getWidth());
        currentPageField.setPrefWidth(previewButton.getWidth());


    }

    private void updateRange(int lower, int upper) {
        lowerRange = lower;
        upperRange = upper;
        currentPage = lower; //set preview to lower bound of the rage
    }

    private void scalePDF() {
        try {
            String scale = scaleTextField.getText();
            scalePages(Float.parseFloat(scale));
        } catch (IllegalArgumentException e1) {
            Alert error = new Alert(Alert.AlertType.ERROR, "Invalid Input", ButtonType.OK);
            error.showAndWait();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * open a pdf
     * @throws IOException
     */
    private void openFile() throws IOException {

//		if (isOpen) {
//			document.close();
//		}

        File file = fileChooser.showOpenDialog(window);
        if (file != null) {
            lastPath = file.getParentFile(); //remember path of the opened file
            fileName = file.getName();
            String ext = FilenameUtils.getExtension(fileName);
            currentPage = 1;
            if (!ext.equals("pdf")) {
                Alert error = new Alert(Alert.AlertType.ERROR, "Wrong file format", ButtonType.OK);
                error.showAndWait();
            } else {

                try {
                    document = PDDocument.load(file);
                    lowerRange = 1; //initial page range
                    upperRange = document.getNumberOfPages();
                    isOpen = true;
                    currentPageField.setText(String.valueOf(currentPage));
                    window.setTitle("PDFManipulator - " + fileName);
                    previewImage();
                } catch (IOException ex) {
                    Alert error = new Alert(Alert.AlertType.ERROR, "Something went wrong", ButtonType.OK);
                    error.showAndWait();
                }

            }
        }

    } // openFile

    /**
     * save the (modified) pdf
     */
    private void saveFile() {

        if (!isOpen) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Open file first", ButtonType.OK);
            alert.showAndWait();
        } else {
            try {
                PDDocument doc = document;
                if (lowerRange != 1 || upperRange != doc.getNumberOfPages()) {
                    //range isnt the entire document
                    doc = getSplitDocument(lowerRange, upperRange);
                }
                fileChooser.setTitle("Save as");
                fileChooser.setInitialDirectory(lastPath);
                fileChooser.setInitialFileName(fileName);
                File saveAs = fileChooser.showSaveDialog(window);
                if (saveAs != null) {
                // document.save(saveAs);
                    doc.save(saveAs);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                Alert error = new Alert(Alert.AlertType.ERROR, "Invalid name", ButtonType.OK);
                error.showAndWait();
            }
        }

    } //saveFile

    /**
     * return a document that only consists of pages i to j (inclusive)
     * @param i first page index
     * @param j last page index
     * @return
     */
    private PDDocument getSplitDocument(int i, int j) {
        PDDocument doc = new PDDocument();
            for(; i<j; i++) {
                doc.addPage(document.getPage(i));
            }
        return doc;
    }

    /**
     * preview the current pdf page
     */
    private void previewImage() {

        if (!isOpen) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Open file first", ButtonType.OK);
            alert.showAndWait();
        } else {

            try {
                PDFRenderer renderer = new PDFRenderer(document);
                preview = renderer.renderImage(currentPage-1); //preview page-1 because variable is 1indexed
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            double a = preview.getHeight();
            double b = preview.getWidth();
            sizeLabel.setText(b +"x"+ a);
            preview = Scalr.resize(preview, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC, 600, 600);
            Image prev = SwingFXUtils.toFXImage(preview, null);


            previewPane.getChildren().clear();
            previewPane.getChildren().add(new ImageView(prev));
            currentPageField.setText(String.valueOf((currentPage)));
            totalPagesLabel.setText(" / " + upperRange);

            window.sizeToScene();
        }


    } //previewImage

    /**
     * rotate the pages of a pdf document
     * @param angle
     * @throws IOException
     */
    private void rotatePages(double angle) throws IOException {
        if (!isOpen) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Open file first", ButtonType.OK);
            alert.showAndWait();
        } else {
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PDPage page = document.getPage(i);
                PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.PREPEND, false, false);

                double theta = Math.toRadians(angle);
                Matrix r1 = Matrix.getRotateInstance(theta, 0, 0);

                transformPDF(page, contentStream, r1);
            }

            previewImage();
        }
    } //of rotatePages


    private void scalePages(float factor) throws IOException {

        if (!isOpen) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Open file first", ButtonType.OK);
            alert.showAndWait();
        } else {
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PDPage page = document.getPage(i);
                PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.PREPEND, false, false);
                Matrix s1 = Matrix.getScaleInstance(factor, factor);
                transformPDF(page, contentStream, s1);
            }
            previewImage();
        }

    }//of scalePages

    /**
     * Transform the content of a pdf page
     * @param page 
     * @param contentStream
     * @param A transformation matrix
     * @throws IOException
     */
    private void transformPDF(PDPage page, PDPageContentStream contentStream, Matrix A) throws IOException {
        contentStream.transform(A);
        contentStream.close();

        PDRectangle cropBox = page.getCropBox();
        Rectangle temp = cropBox.transform(A).getBounds(); //boundaries of result
        PDRectangle newCropBox = new PDRectangle((float) temp.getX(), (float) temp.getY(), (float) temp.getWidth(), (float) temp.getHeight());
        page.setCropBox(newCropBox);
        page.setMediaBox(newCropBox);
    }


    private PDDocument splitH() throws IOException {
        PDDocument resultDoc = new PDDocument();

        for (int i = 0; i < document.getNumberOfPages(); i++) {

            PDPage page = document.getPage(i);
            PDRectangle cropBox = page.getCropBox();
            float refY = cropBox.getLowerLeftY();

            float boxHeight = cropBox.getHeight();

            //upper
            cropBox = page.getCropBox();
            cropBox.setLowerLeftY((refY + boxHeight / 2));
            //up right stays
            page.setCropBox(cropBox);
            page.setMediaBox(cropBox);
            resultDoc.importPage(page);


            //lower
            cropBox = page.getCropBox();
            cropBox.setLowerLeftY(refY);
            cropBox.setUpperRightY((refY + boxHeight / 2));
            page.setCropBox(cropBox);
            page.setMediaBox(cropBox);
            resultDoc.importPage(page);

        }
        return resultDoc;

    }


    private PDDocument splitV() throws IOException {
        PDDocument resDoc = new PDDocument();

        for (int i = 0; i < document.getNumberOfPages(); i++) {
            PDPage page = document.getPage(i);
            PDRectangle cropBox = page.getCropBox();


            float refX = cropBox.getLowerLeftX();
            float boxWidth = cropBox.getWidth();

            //left half
            cropBox = page.getCropBox();
            cropBox.setUpperRightX((refX + boxWidth) / 2);
            page.setCropBox(cropBox);
            page.setMediaBox(cropBox);
            resDoc.importPage(page);

            //right half
            cropBox = page.getCropBox();
            cropBox.setLowerLeftX((refX + boxWidth) / 2);
            cropBox.setUpperRightX(boxWidth);
            page.setCropBox(cropBox);
            page.setMediaBox(cropBox);
            resDoc.importPage(page);

        }
        return resDoc;
    }

    /**
     * split pages of the pdf
     * @param where
     * @throws IOException
     */
    private void splitPages(String where) throws IOException {

        if (!isOpen) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Open file first", ButtonType.OK);
            alert.showAndWait();
        } else {
            if (where.equals("horizontally")) {

                document = splitH();
            } else if (where.equals("vertically")) {
                document = splitV();
            }

        }


    } //of splitPages

}


