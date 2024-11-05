package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ModifyMavenSetting {
    private Document document;
    private final String pomFilePath;

    // 接收 pom.xml route
    public ModifyMavenSetting(String pomFilePath) {
        this.pomFilePath = pomFilePath;
    }

    // load pom.xml
    public void loadPomFile() throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder(); // 將 XML 轉換成 JDOM 的 Document 物件
        File pomFile = new File(pomFilePath);
        document = saxBuilder.build(pomFile); // document 會包含 pom.xml 的所有內容，可以使用 JDOM API 操作
        System.out.println("POM file loaded successfully.");
    }

    public void modifyArtifactId(String newArtifactId) {
        modifyElement("artifactId", newArtifactId);
    }

    public void modifyName(String newName) {
        modifyElement("name", newName);
    }

    private void modifyElement(String elementName, String newValue) {
        if (document == null) {
            System.out.println("Document not loaded. Call loadPomFile() first.");
            return;
        }

        Element rootElement = document.getRootElement(); // `<project>` element
        Element ModifiedElement = rootElement.getChild(elementName, rootElement.getNamespace());

        if (ModifiedElement != null) {
            ModifiedElement.setText(newValue);
            System.out.println(elementName + " has been modified successfully: " + newValue);
        } else {
            System.out.printf("Cannot find %s element", elementName);
        }
    }

    // save pom.xml
    public void savePomFile() throws IOException {
        if (document == null) {
            System.out.println("Document not loaded. Call loadPomFile() first.");
            return;
        }

        XMLOutputter xmlOutputter = new XMLOutputter();
        xmlOutputter.setFormat(Format.getPrettyFormat());
        xmlOutputter.output(document, new FileOutputStream(pomFilePath));
        System.out.println("POM file saved successfully.");
    }
}
