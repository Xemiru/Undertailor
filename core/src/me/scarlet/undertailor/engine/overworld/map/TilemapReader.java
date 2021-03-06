/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Tellerva, Marc Lawrence
 *
 * Permission is hereby granted, free of charge, to any
 * person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the
 * Software without restriction, including without
 * limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice
 * shall be included in all copies or substantial portions
 * of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package me.scarlet.undertailor.engine.overworld.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import me.scarlet.undertailor.AssetManager;
import me.scarlet.undertailor.engine.Layerable;
import me.scarlet.undertailor.engine.overworld.map.ObjectLayer.ShapeData;
import me.scarlet.undertailor.engine.overworld.map.TilemapFactory.Tilemap;
import me.scarlet.undertailor.engine.overworld.map.TilesetFactory.Tileset;
import me.scarlet.undertailor.exception.BadAssetException;
import me.scarlet.undertailor.gfx.MultiRenderer;
import me.scarlet.undertailor.util.Wrapper;
import me.scarlet.undertailor.util.XMLUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.Comparator;

import javax.xml.parsers.SAXParser;

/**
 * {@link DefaultHandler} implementation for reading Tiled
 * .tmx files.
 */
public class TilemapReader extends DefaultHandler {

    static final Logger log = LoggerFactory.getLogger(TilemapReader.class);
    static final String[] supportedCompression = {"csv", "base64"};

    static final Comparator<Layerable> LAYER_COMPARATOR;

    static {
        LAYER_COMPARATOR = (l1, l2) -> {
            return Short.compare(l1.getLayer(), l2.getLayer());
        };
    }

    private TilesetManager tilesets;

    // processing vars;
    private Array<String> tree;

    private short layerId;
    private Tilemap tilemap;
    private SAXParser parser;
    private MultiRenderer renderer;
    private TileLayer currentTileLayer;
    private ImageLayer currentImageLayer;
    private ObjectLayer currentObjectLayer;

    private ShapeData currentShape;
    private Wrapper<Object> wrapper;
    private String layerName;
    private String tileData;
    private String comp;

    public TilemapReader(TilesetManager tilesets, MultiRenderer renderer) {
        tree = new Array<>(true, 16);
        this.tilesets = tilesets;
        this.renderer = renderer;

        this.tilemap = null;
        this.currentTileLayer = null;
        this.tileData = null;
        this.comp = null;
        this.parser = XMLUtil.generateParser();
        this.wrapper = new Wrapper<>();
    }

    // ---------------- abstract method implementation ----------------

    @Override
    public void startDocument() throws SAXException {
        this.layerId = 0;
        this.tree.clear();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException {
        this.tree.add(qName);

        if (this.checkElement("", "map", qName)) {
            tilemap.width = Integer.parseInt(attributes.getValue("", "width"));
            tilemap.height = Integer.parseInt(attributes.getValue("", "height"));
        }

        if (this.checkElement("map", "tileset", qName)) {
            String name = attributes.getValue("", "name");
            if (name == null) {
                String[] split = attributes.getValue("", "source").split("/");
                name = split[split.length - 1];
                name = name.substring(0, name.length() - 4);
            }

            Tileset tileset = this.tilesets.getTileset(name);
            if (tileset == null) {
                throw new SAXException(
                    new BadAssetException("Could not find tileset under id " + name));
            }

            this.tilemap.tilesets.put(Integer.parseInt(attributes.getValue("", "firstgid")),
                tileset);
        }

        if (this.checkElement("map", "layer", qName)) {
            this.currentTileLayer = new TileLayer();
            this.currentTileLayer.name = attributes.getValue("", "name");
            this.currentTileLayer.id = this.layerId++;
            this.currentTileLayer.parent = this.tilemap;
            this.layerName =
                this.currentTileLayer.name == null ? "unnamed layer" : this.currentTileLayer.name;
        }

        if (this.checkElement("map", "imagelayer", qName)) {
            this.currentImageLayer = new ImageLayer();
            this.currentImageLayer.id = this.layerId++;

            String name = attributes.getValue("", "name");
            String sX = attributes.getValue("", "offsetx");
            String sY = attributes.getValue("", "offsety");
            float x = sX == null ? 0 : Float.parseFloat(sX);
            float y = sY == null ? 0 : this.toGDXPoint(Float.parseFloat(sY));

            this.layerName = name == null ? "unnamed" : name;
            this.currentImageLayer.setPosition(x, y);
        }

        if (this.currentTileLayer != null && this.checkElement("layer", "data", qName)) {
            String compression = attributes.getValue("", "encoding");
            if (!this.isCompressionSupported(compression)) {
                throw new UnsupportedOperationException(
                    "Undertailor does not support Tiled maps saved in " + compression + " format");
            }

            this.comp = compression;
            this.tileData = "";
        }

        if (this.currentImageLayer != null && this.checkElement("imagelayer", "image", qName)) {
            this.wrapper.set(null);
            String[] sourceNameSplit = attributes.getValue("", "source").split("/");
            File file =
                new File(
                    new File(AssetManager.rootDirectory.getAbsolutePath(),
                        AssetManager.DIR_TILEMAP_IMAGES),
                    sourceNameSplit[sourceNameSplit.length - 1]);
            AssetManager.addTask(() -> {
                synchronized (wrapper) {
                    this.wrapper.set(new Texture(Gdx.files.absolute(file.getAbsolutePath())));
                    this.wrapper.notifyAll();
                }
            });

            synchronized (wrapper) {
                while (wrapper.get() == null) {
                    try {
                        wrapper.wait();
                    } catch (InterruptedException ignore) {
                    }
                }
            }

            this.currentImageLayer.image = (Texture) this.wrapper.get();
            this.currentImageLayer.position.y =
                this.currentImageLayer.position.y - ((Texture) this.wrapper.get()).getHeight();
        }

        if (this.checkElement("map", "objectgroup", qName)) {
            this.currentObjectLayer = new ObjectLayer();
            this.currentObjectLayer.name = attributes.getValue("", "name");
        }

        if (this.checkElement("objectgroup", "object", qName)) {
            // will always have these
            this.currentShape = new ShapeData();
            this.currentShape.name = attributes.getValue("", "name");
            this.currentShape.position.x = Float.parseFloat(attributes.getValue("", "x"));
            this.currentShape.position.y =
                this.toGDXPoint(Float.parseFloat(attributes.getValue("", "y")));

            // only circle/rectangle have these
            String shapeWidth = attributes.getValue("", "width");
            this.currentShape.shapeWidth = shapeWidth == null ? 0 : Float.valueOf(shapeWidth);

            String shapeHeight = attributes.getValue("", "height");
            this.currentShape.shapeHeight = shapeHeight == null ? 0 : Float.valueOf(shapeHeight);

            String type = attributes.getValue("", "type");
            if (type != null && type.equalsIgnoreCase("entrypoint")) {
                this.tilemap.entrypoints.put(this.getQualifiedShapeName(), "");
            }
        }

        if (this.checkElement("object", "polygon", qName)
            || this.checkElement("object", "polyline", qName)) {
            String[] points = attributes.getValue("", "points").split(" ");
            this.currentShape.shapeVertices = new float[points.length * 2];
            for (int i = 0; i < points.length; i++) {
                String[] point = points[i].split(",");
                this.currentShape.shapeVertices[i * 2] = Float.parseFloat(point[0]);
                this.currentShape.shapeVertices[(i * 2) + 1] = Float.parseFloat(point[1]);
            }
        }

        // object custom props
        if (this.currentShape != null && this.checkElement("properties", "property", qName)) {
            if (attributes.getValue("", "name").equalsIgnoreCase("spawn")) {
                String targetPoint = attributes.getValue("", "value");
                if (targetPoint.split(":").length == 1) { // if they didn't qualify the name
                    targetPoint = this.currentObjectLayer.name + ":" + targetPoint; // qualify it for them, defaulting to the current layer
                }

                this.tilemap.entrypoints.put(this.getQualifiedShapeName(), targetPoint);
            }
        }

        // tile layer custom props
        if (this.currentTileLayer != null && this.checkElement("properties", "property", qName)) {
            if (attributes.getValue("", "name").equals("layer")) {
                String value = attributes.getValue("", "value");
                try {
                    this.currentTileLayer.layer = Short.parseShort(value);
                    this.currentTileLayer.layerSet = true;
                } catch (NumberFormatException e) {
                    log.warn("Invalid layer value \"" + value + "\" for tile layer " + layerName);
                    log.warn("Skipping tile layer \"" + layerName + "\"");
                    this.currentTileLayer = null;
                    this.layerName = null;
                    this.tileData = null;
                }
            }
        }

        // image layer custom props
        if (this.currentImageLayer != null && this.checkElement("properties", "property", qName)) {
            String key = attributes.getValue("", "name");
            String value = attributes.getValue("", "value");
            if (key.equals("layer")) {
                try {
                    this.currentImageLayer.layer = Short.parseShort(value);
                    this.currentImageLayer.layerSet = true;
                } catch (NumberFormatException e) {
                    log.warn("Invalid layer value \"" + value + "\" for image layer " + layerName);
                    log.warn("Skipping image layer \"" + layerName + "\"");
                    this.currentImageLayer = null;
                }
            }

            if (this.currentImageLayer != null && key.equals("threshold")) {
                try {
                    float threshold = Float.parseFloat(value);
                    if (threshold > 1.0)
                        threshold = 1F;
                    if (threshold < 0)
                        threshold = 0F;
                    this.currentImageLayer.threshold = threshold;
                } catch (NumberFormatException e) {
                    log.warn("Invalid threshold value " + value + " for image layer " + layerName);
                    log.warn(layerName + " threshold value defaulted to 0");
                }
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (this.tileData != null) {
            for (int i = 0; i < length; i++) {
                char character = ch[i + start];
                if (character == '\n' || character == ' ') {
                    continue;
                } else {
                    this.tileData += character;
                }
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (this.checkElement("layer", "data", qName)) {
            this.currentTileLayer.tiles = this.parseData(this.comp, this.tileData);
            this.tileData = null;
            this.comp = null;
        }

        if (this.checkElement("map", "layer", qName)) {
            if (this.currentTileLayer != null) {
                if (!this.currentTileLayer.layerSet) {
                    log.warn("Layer \"" + layerName + "\" is missing layer property");
                    log.warn("Skipping tile layer \"" + layerName + "\"");
                } else {
                    this.tilemap.tileLayers.add(this.currentTileLayer);
                }

                this.currentTileLayer = null;
            }
        }

        if (this.checkElement("object", "polygon", qName)) {
            if (this.currentShape.shapeVertices.length > 16) {
                throw new UnsupportedOperationException(
                    "libGDX's Box2D will not permit polygons with more than 8 vertices");
            }

            this.currentShape.type = Shape.Type.Polygon;
        }

        if (this.checkElement("object", "polyline", qName)) {
            this.currentShape.type = Shape.Type.Chain;
        }

        if (this.checkElement("object", "ellipse", qName)) {
            this.currentShape.type = Shape.Type.Circle;
        }

        if (this.checkElement("objectgroup", "object", qName)) {
            if (this.currentShape.name != null
                && (this.currentShape.type == null || this.currentShape.type == Shape.Type.Circle)
                && (this.currentShape.shapeWidth == 0 && this.currentShape.shapeHeight == 0)) {
                this.currentObjectLayer.points.put(this.currentShape.name,
                    this.currentShape.position);
            } else {
                this.currentShape.generateVertices();
                this.currentObjectLayer.shapes.add(this.currentShape);
            }

            this.currentShape = null;
        }

        if (this.checkElement("map", "objectgroup", qName)) {
            this.tilemap.objects.put(this.currentObjectLayer.name, this.currentObjectLayer);
            this.currentObjectLayer = null;
        }

        if (this.checkElement("map", "imagelayer", qName)) {
            if (this.currentImageLayer != null) {
                if (!this.currentImageLayer.layerSet) {
                    log.warn("Layer \"" + layerName + "\" is missing layer property");
                    log.warn("Skipping image layer \"" + layerName + "\"");
                } else {
                    this.currentImageLayer.renderer = this.renderer;
                    this.tilemap.imageLayers.add(this.currentImageLayer);
                }

                this.currentImageLayer = null;
            }
        }

        this.tree.removeValue(qName, false);
    }

    @Override
    public void endDocument() throws SAXException {
        // sort the maps and things
        this.tilemap.tileLayers.sort(TilemapReader.LAYER_COMPARATOR);
        this.tilemap.imageLayers.sort(TilemapReader.LAYER_COMPARATOR);
        this.tilemap.tilesets.orderedKeys().sort();

        // check the entrypoints
        // we have to access the layer list directly, since the method does not return anything if the map hasn't been deemed fully loaded
        ObjectMap<String, ObjectLayer> layers = this.tilemap.objects;
        for (ObjectMap.Entry<String, String> entry : this.tilemap.entrypoints.entries()) {
            // check that the entrypoint itself is a shape, not a point
            ObjectLayer layer;
            String sLayer = entry.key.split(":")[0];
            String sName = entry.key.split(":")[1];
            layer = layers.get(sLayer);

            if (sLayer.charAt(0) != TilemapFactory.OBJ_DEF_LAYER_PREFIX) {
                throw new SAXException(
                    "Entrypoints may only be defined within definition layers (offending point "
                        + sLayer + ":" + (sName.equalsIgnoreCase("null") ? "" : sName) + ")");
            }

            if (layer == null || layer.getShape(sName) == null) {
                throw new SAXException("Entrypoint " + entry.key + " is not a shape");
            }

            // check that the target has a point
            if (entry.value == null || entry.value.trim().isEmpty()) {
                continue;
            } else {
                String pLayer = entry.value.split(":")[0];
                String pName = entry.value.split(":")[1];
                layer = layers.get(pLayer);

                if (layer == null || layer.getPoint(pName) == null) {
                    throw new SAXException("Entrypoint " + entry.key
                        + " resolves to invalid spawn point " + entry.value);
                }
            }
        }
    }

    // ---------------- object ----------------

    /**
     * Reads the provided .tmx file and provides a
     * {@link Tilemap} object providing the loaded tilemap.
     * 
     * @param tmxFile the File pointing to the target .tmx
     *        file
     * @param tilemap the Tilemap to load the map data into
     * 
     * @return the provided Tilemap, now loaded
     * 
     * @throws FileNotFoundException if the .tmx file was
     *         not found
     * @throws SAXException if an XML parsing error occurred
     * @throws IOException if a miscellaneous I/O error
     *         occured
     */
    public Tilemap read(File tmxFile, Tilemap tilemap)
        throws FileNotFoundException, SAXException, IOException {
        FileInputStream stream = null;
        this.tilemap = tilemap;

        try {
            stream = new FileInputStream(tmxFile);
            InputStreamReader reader = new InputStreamReader(stream);
            InputSource source = new InputSource(reader);

            parser.parse(source, this);
        } finally {
            if (stream != null)
                stream.close();
        }

        return this.tilemap;
    }

    // ---------------- internal methods ----------------

    /**
     * Internal method.
     * 
     * <p>Returns the parent element of the current element,
     * or an empty string if the element has no parent (if
     * it is the root node).</p>
     */
    private String getParentElement() {
        if (this.tree.size == 1) {
            return "";
        }

        return this.tree.get(this.tree.size - 2);
    }

    /**
     * Internal method.
     * 
     * <p>Returns the qualified name of the current shape.
     * Will return null if there is no current shape or
     * object layer.</p>
     */
    private String getQualifiedShapeName() {
        if (this.currentObjectLayer == null || this.currentShape == null) {
            return null;
        }

        return this.currentObjectLayer.name + ":" + this.currentShape.name;
    }

    /**
     * Internal method.
     * 
     * <p>Convenience method to quickly check an element
     * before processing it.</p>
     * 
     * @param parent the required parent name
     * @param tag the required element name
     * @param qname the qualified name of the current
     *        element
     */
    private boolean checkElement(String parent, String tag, String qName) {
        if (this.getParentElement().equals(parent) && qName.equals(tag)) {
            return true;
        }

        return false;
    }

    private boolean isCompressionSupported(String str) {
        for (String comp : supportedCompression) {
            if (comp.equalsIgnoreCase(str)) {
                return true;
            }
        }

        return false;
    }

    private int[] parseData(String comp, String tileData) {
        int[] tiles = null;
        if (comp.equalsIgnoreCase("csv")) {
            String[] tileSplit = tileData.split(",");
            tiles = new int[tileSplit.length];

            for (int i = 0; i < tiles.length; i++) {
                tiles[i] = Integer.parseInt(tileSplit[i]);
            }
        }

        if (comp.equalsIgnoreCase("base64")) {
            ByteBuffer buf = ByteBuffer.wrap(Base64.getDecoder().decode(tileData));
            buf.order(ByteOrder.LITTLE_ENDIAN);
            int count = buf.limit() / 4;
            tiles = new int[count];
            for (int i = 0; i < count; i++) {
                tiles[i] = buf.getInt();
            }
        }

        return tiles;
    }

    float toGDXPoint(float y) {
        return this.tilemap.getOccupiedHeight() - y;
    }
}
