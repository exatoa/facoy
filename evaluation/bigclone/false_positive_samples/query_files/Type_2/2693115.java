package org.matsim.vis.kml;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.LinkType;
import net.opengis.kml._2.NetworkLinkType;
import net.opengis.kml._2.ObjectFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.core.api.internal.MatsimSomeWriter;

/**
 * A writer for complex keyhole markup files used by Google Earth. It supports
 * packing multiple kml-files into one zip-compressed file which can directly be
 * read by Google Earth. The files will have the ending *.kmz.
 *
 * @author mrieser
 *
 */
public class KMZWriter implements MatsimSomeWriter {

    private static final Logger log = Logger.getLogger(KMZWriter.class);

    private BufferedWriter out = null;

    private ZipOutputStream zipOut = null;

    private final Map<String, String> nonKmlFiles = new HashMap<String, String>();

    private static final Marshaller marshaller;

    private static final ObjectFactory kmlObjectFactory = new ObjectFactory();

    static {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance("net.opengis.kml._2");
            marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * Creates a new kmz-file and a writer for it and opens the file for writing.
	 *
	 * @param outFilename
	 *          the location of the file to be written.
	 */
    public KMZWriter(final String outFilename) {
        log.setLevel(Level.INFO);
        String filename = outFilename;
        if (filename.endsWith(".kml") || filename.endsWith(".kmz")) {
            filename = filename.substring(0, filename.length() - 4);
        }
        try {
            this.zipOut = new ZipOutputStream(new FileOutputStream(filename + ".kmz"));
            this.out = new BufferedWriter(new OutputStreamWriter(this.zipOut, "UTF8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        KmlType docKML = kmlObjectFactory.createKmlType();
        NetworkLinkType nl = kmlObjectFactory.createNetworkLinkType();
        LinkType link = kmlObjectFactory.createLinkType();
        link.setHref("main.kml");
        nl.setLink(link);
        docKML.setAbstractFeatureGroup(kmlObjectFactory.createNetworkLink(nl));
        writeKml("doc.kml", docKML);
    }

    /**
	 * Adds the specified KML-object to the file.
	 *
	 * @param filename
	 *          The internal filename of this kml-object in the kmz-file. Other
	 *          kml-objects in the same kmz-file can reference this kml with the
	 *          specified filename.
	 * @param kml
	 *          The KML-object to store in the file.
	 */
    public void writeLinkedKml(final String filename, final KmlType kml) {
        if (filename.equals("doc.kml")) {
            throw new IllegalArgumentException("The filename 'doc.kml' is reserved for the primary kml.");
        }
        if (filename.equals("main.kml")) {
            throw new IllegalArgumentException("The filename 'main.kml' is reserved for the main kml.");
        }
        writeKml(filename, kml);
    }

    /**
	 * Writes the specified KML-object as the main kml into the file. The main kml
	 * is the one Google Earth reads when the file is opened. It should contain
	 * NetworkLinks to the other KMLs stored in the same file.
	 *
	 * @param kml
	 *          the KML-object that will be read by Google Earth when opening the
	 *          file.
	 */
    public void writeMainKml(final KmlType kml) {
        writeKml("main.kml", kml);
    }

    /**
	 * Closes this file for writing.
	 */
    public void close() {
        try {
            this.out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Adds a file to the kmz which is not a kml file.
	 *
	 * @param filename the path to the file, relative or absolute
	 * @param inZipFilename the filename used for the file in the kmz file
	 * @throws IOException
	 */
    public void addNonKMLFile(final String filename, final String inZipFilename) throws IOException {
        if (this.nonKmlFiles.containsKey(filename) && (inZipFilename.compareTo(this.nonKmlFiles.get(filename)) == 0)) {
            log.warn("File: " + filename + " is already included in the kmz as " + inZipFilename);
            return;
        }
        this.nonKmlFiles.put(filename, inZipFilename);
        FileInputStream fis = new FileInputStream(filename);
        try {
            addNonKMLFile(fis, inZipFilename);
        } finally {
            fis.close();
        }
    }

    /**
	 * Adds some data as a file to the kmz. The data stream will be closed at the end of the method.
	 *
	 * @param data the data to add to the kmz
	 * @param inZipFilename the filename used for the file in the kmz file
	 * @throws IOException
	 */
    public void addNonKMLFile(final InputStream data, final String inZipFilename) throws IOException {
        try {
            byte[] buffer = new byte[4096];
            int bytesRead;
            ZipEntry entry = new ZipEntry(inZipFilename);
            this.zipOut.putNextEntry(entry);
            while ((bytesRead = data.read(buffer)) != -1) {
                this.zipOut.write(buffer, 0, bytesRead);
            }
            log.debug(entry.getName() + " added to kmz.");
        } finally {
            data.close();
        }
    }

    /**
	 * Adds a file (in form of a byte array) to a kml file.
	 * @param data
	 * @param inZipFilename inZipFilename the filename used for the file in the kmz file
	 * @throws IOException
	 */
    public void addNonKMLFile(final byte[] data, final String inZipFilename) throws IOException {
        ZipEntry entry = new ZipEntry(inZipFilename);
        this.zipOut.putNextEntry(entry);
        this.zipOut.write(data);
        log.debug(entry.getName() + " added to kmz.");
    }

    /**
	 * internal routine that does the real writing of the data
	 *
	 * @param filename
	 * @param kml
	 */
    private void writeKml(final String filename, final KmlType kml) {
        try {
            ZipEntry ze = new ZipEntry(filename);
            ze.setMethod(ZipEntry.DEFLATED);
            this.zipOut.putNextEntry(ze);
            try {
                marshaller.marshal(kmlObjectFactory.createKml(kml), out);
            } catch (JAXBException e) {
                e.printStackTrace();
            }
            this.out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
