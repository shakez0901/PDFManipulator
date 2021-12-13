package sample;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
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
	private Button saveButton, openButton, prevPageButton, nextPageButton,
			firstPageButton, lastPageButton, scaleButton, rotateLeftButton,
			rotateRightButton, rotate180Button, splitMidHorButton, splitMidVertButton, previewButton;

	private TilePane previewPane;
	private TextField scaleTextField, currentPageField;
	private Label totalPagesLabel;
	private FileChooser fileChooser;

	private boolean isOpen = false;

	private int currentPage;
	private PDDocument document;
	private BufferedImage preview;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
				new FileChooser.ExtensionFilter("All Files", "*.*"));
		fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "/Desktop"));

		window = primaryStage;
		window.setTitle("PDFManipulator");

		saveButton = new Button("Save");
		saveButton.setOnAction(e -> saveFile());

		openButton = new Button("Open");
		openButton.setOnAction(e -> {
			try {
				openFile();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		});

		prevPageButton = new Button("<");
		prevPageButton.setOnAction(e -> {
			currentPage--;
			if (currentPage < 0) {
				currentPage = document.getNumberOfPages() - 1;
			}
			previewImage();
		});

		nextPageButton = new Button(">");
		nextPageButton.setOnAction(e -> {
			currentPage++;
			if (currentPage > document.getNumberOfPages() - 1) {
				currentPage = 0;
			}
			previewImage();
		});

		firstPageButton = new Button("<<");
		firstPageButton.setOnAction(e -> {
			currentPage = 0;
			previewImage();
		});

		lastPageButton = new Button(">>");
		lastPageButton.setOnAction(e -> {
			currentPage = document.getNumberOfPages() - 1;
			previewImage();
		});

		rotate180Button = new Button("Rotate 180°");
		rotate180Button.setOnAction(e -> {

			try {
				rotatePages(180);
			} catch (IOException ex) {
				ex.printStackTrace();
			}

		});

		rotateLeftButton = new Button("Rotate left");
		rotateLeftButton.setOnAction(e -> {

			try {
				rotatePages(90);
			} catch (IOException ex) {
				ex.printStackTrace();
			}

		});

		rotateRightButton = new Button("Rotate right");
		rotateRightButton.setOnAction(e -> {


			try {
				rotatePages(-90);
			} catch (IOException ex) {
				ex.printStackTrace();
			}

		});


		splitMidVertButton = new Button("Split vertically");
		splitMidVertButton.setOnAction(e -> {
			try {
				splitPages("vertically");
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		});


		splitMidHorButton = new Button("Split horizontally");
		splitMidHorButton.setOnAction(e -> {
			try {
				splitPages("horizontally");
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		});


		scaleButton = new Button("Scale");
		scaleButton.setOnAction(e -> {
			try {
				String scale = scaleTextField.getText();
				scalePages(Float.parseFloat(scale));
			} catch (IllegalArgumentException e1) {
				Alert error = new Alert(Alert.AlertType.ERROR, "Invalid Input", ButtonType.OK);
				error.showAndWait();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		});
		scaleTextField = new TextField();
		scaleTextField.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ENTER) {
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
		});


		previewButton = new Button("Preview");
		previewButton.setOnAction(e -> {
			String text = currentPageField.getText();
			if (text.equals("")) {
				currentPage = 0;
			} else {
				try {
					currentPage = Integer.parseInt(text);
					if (currentPage > document.getNumberOfPages() - 1) {
						currentPage = document.getNumberOfPages() - 1;
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
					currentPage = Integer.parseInt(currentPageField.getText()) - 1;
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


		previewPane = new TilePane();
		previewPane.setPrefColumns(1);
		previewPane.setPrefRows(1);

		VBox layout1 = new VBox(15);

		HBox box1 = new HBox(15);
		box1.getChildren().addAll(saveButton, openButton);

		//layout for navigation
		HBox box2 = new HBox(15);
		box2.getChildren().addAll(firstPageButton, prevPageButton, nextPageButton, lastPageButton);

		//layout for manipulation buttons
		HBox box3 = new HBox(15);
		VBox scaleBox = new VBox();
		scaleBox.getChildren().addAll(scaleButton, scaleTextField);
		box3.getChildren().addAll(scaleBox, rotateLeftButton, rotateRightButton, rotate180Button, splitMidHorButton, splitMidVertButton);

		HBox box4 = new HBox();
		box4.getChildren().addAll(currentPageField, totalPagesLabel);
		VBox prevBox = new VBox();
		prevBox.getChildren().addAll(previewButton, box4);

		//add everything to the layout
		layout1.getChildren().addAll(box1, box3, prevBox, previewPane, box2);


		Scene scene = new Scene(layout1);
		window.setScene(scene);
		window.show();

		//match size of textfield with corresponding button
		scaleTextField.setPrefWidth(scaleButton.getWidth());
		currentPageField.setPrefWidth(previewButton.getWidth());


	}

	private void openFile() throws IOException {

//		if (isOpen) {
//			document.close();
//		}

		File file = fileChooser.showOpenDialog(window);
		if (file != null) {
			System.out.println("called open");
			currentPage = 0;
			String ext = FilenameUtils.getExtension(file.getName());

			if (!ext.equals("pdf")) {
				Alert error = new Alert(Alert.AlertType.ERROR, "Wrong file format", ButtonType.OK);
				error.showAndWait();
			} else {

				try {
					document = PDDocument.load(file);
					isOpen = true;
					currentPageField.setText(String.valueOf(currentPage));
					window.setTitle("PDFManipulator - " + file.getName());
					previewImage();
				} catch (IOException ex) {
					ex.printStackTrace();
				}

			}
		}

	} // openFile

	private void saveFile() {

		if (!isOpen) {
			Alert alert = new Alert(Alert.AlertType.WARNING, "Open file first", ButtonType.OK);
			alert.showAndWait();
//			JOptionPane.showMessageDialog(null, "Open file first", "Error", JOptionPane.ERROR_MESSAGE);
		} else {
			try {
				fileChooser.setTitle("Save as");
				File saveAs = fileChooser.showSaveDialog(window);
				if (saveAs != null) {
					document.save(saveAs + ".pdf");
				}


			} catch (IOException ex) {
				ex.printStackTrace();
//				JOptionPane.showMessageDialog(null, "Invalid Name", "Error", JOptionPane.ERROR_MESSAGE);
				Alert error = new Alert(Alert.AlertType.ERROR, "Invalid name", ButtonType.OK);
				error.showAndWait();
			}
		}

	} //saveFile

	private void previewImage() {

		if (!isOpen) {
			Alert alert = new Alert(Alert.AlertType.WARNING, "Open file first", ButtonType.OK);
			alert.showAndWait();
		} else {

			try {
				PDFRenderer renderer = new PDFRenderer(document);
				preview = renderer.renderImage(currentPage);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			preview = Scalr.resize(preview, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC, 600, 600);
			Image prev = SwingFXUtils.toFXImage(preview, null);


			previewPane.getChildren().clear();
			previewPane.getChildren().add(new ImageView(prev));
			currentPageField.setText(String.valueOf((currentPage + 1)));
			totalPagesLabel.setText(" / " + document.getNumberOfPages());

			window.sizeToScene();
		}


	} //previewImage


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

				contentStream.transform(r1);
				contentStream.close();

				PDRectangle cropBox = page.getCropBox();
				Rectangle temp = cropBox.transform(r1).getBounds(); //boundaries of result
				PDRectangle newCropBox = new PDRectangle((float) temp.getX(), (float) temp.getY(), (float) temp.getWidth(), (float) temp.getHeight());
				page.setCropBox(newCropBox);
				page.setMediaBox(newCropBox);
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

				contentStream.transform(s1);
				contentStream.close();

				PDRectangle cropBox = page.getCropBox();
				Rectangle temp = cropBox.transform(s1).getBounds(); //boundaries of result
				PDRectangle newCropBox = new PDRectangle((float) temp.getX(), (float) temp.getY(), (float) temp.getWidth(), (float) temp.getHeight());
				page.setCropBox(newCropBox);
				page.setMediaBox(newCropBox);
			}

			previewImage();
		}

	}//of scalePages


	private PDDocument splitHori() throws IOException {
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


	private PDDocument splitVert() throws IOException {
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

	private void splitPages(String where) throws IOException {

		if (!isOpen) {
			Alert alert = new Alert(Alert.AlertType.WARNING, "Open file first", ButtonType.OK);
			alert.showAndWait();
		} else {

//			PDDocument newDoc = new PDDocument();






			if (where.equals("horizontally")) {

				document = splitHori();
			} else if (where.equals("vertically")) {
				document = splitVert();
			}

//			document = newDoc;
//			document.close();
//			document = new PDDocument(newDoc.getDocument());
//			newDoc.close();
//			System.out.println("doc bef "+document.getNumberOfPages());
//			PDFCloneUtility test = new PDFCloneUtility(document);
//			test.cloneForNewDocument(newDoc.getDocument());
//			newDoc.close();
//			System.out.println("doc after cloe" + document.getNumberOfPages());
		}


	} //of splitPages

}


//
// old
//
//	private void splitPages(String where) throws IOException {
//
//		if (!isOpen) {
//			Alert alert = new Alert(Alert.AlertType.WARNING, "Open file first", ButtonType.OK);
//			alert.showAndWait();
//		} else {
//			PDDocument newDoc = new PDDocument();
//
//			for (int i = 0; i < document.getNumberOfPages(); i++) {
//
//				PDPage page = document.getPage(i);
//				PDRectangle cropBox = page.getCropBox();
//
//
//				if (where.equals("horizontally")) {
//
//					float refY = cropBox.getLowerLeftY();
//
//					float boxHeight = cropBox.getHeight();
//
//					//upper
//					cropBox = page.getCropBox();
//					cropBox.setLowerLeftY((refY + boxHeight / 2));
//					//up right stays
//					page.setCropBox(cropBox);
//					page.setMediaBox(cropBox);
//					newDoc.importPage(page);
//
//
//					//lower
//					cropBox = page.getCropBox();
//					cropBox.setLowerLeftY(refY);
//					cropBox.setUpperRightY((refY + boxHeight / 2));
//					page.setCropBox(cropBox);
//					page.setMediaBox(cropBox);
//					newDoc.importPage(page);
//
//				} else if (where.equals("vertically")) {
//
//					float refX = cropBox.getLowerLeftX();
//					float boxWidth = cropBox.getWidth();
//
//					//left half
//					cropBox = page.getCropBox();
//					cropBox.setUpperRightX((refX+boxWidth)/2);
//					page.setCropBox(cropBox);
//					page.setMediaBox(cropBox);
//					newDoc.importPage(page);
//
//					//right half
//					cropBox=page.getCropBox();
//					cropBox.setLowerLeftX((refX+boxWidth)/2);
//					cropBox.setUpperRightX(boxWidth);
//					page.setCropBox(cropBox);
//					page.setMediaBox(cropBox);
//					newDoc.importPage(page);
//
//				}
////
//
//
//			}
//
//
//////			document.clone()
////			previewImage();
////			document.close();
//			document = newDoc;//			this.document = newDoc;
////			newDoc.close();
//		}
//	} //of splitPages
