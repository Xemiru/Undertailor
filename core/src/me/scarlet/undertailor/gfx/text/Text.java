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

package me.scarlet.undertailor.gfx.text;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.OrderedMap;
import com.badlogic.gdx.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.scarlet.undertailor.Undertailor;
import me.scarlet.undertailor.gfx.Renderable;
import me.scarlet.undertailor.gfx.Transform;
import me.scarlet.undertailor.gfx.spritesheet.Sprite;
import me.scarlet.undertailor.gfx.text.TextStyle.DisplayMeta;
import me.scarlet.undertailor.gfx.text.parse.ParsedText;
import me.scarlet.undertailor.util.CollectionUtil;
import me.scarlet.undertailor.util.Pair;

import java.util.function.BiConsumer;

/**
 * A parent {@link TextComponent} component withholding a
 * number of child component modules to resolve to a full
 * batch of customizable segments.
 */
public class Text extends TextComponent implements Renderable {

    static final char SPACE = ' ';
    static final char NEWLINE = '\n';

    /**
     * Builder-type class for building {@link Text}s.
     */
    public static class Builder extends TextComponent.Builder {

        public Builder() {
            super(new Text());
        }

        /**
         * {@inheritDoc}
         * 
         * <p>For the {@link Text} builder, this method does
         * nothing, as the text for a Text object is a
         * combination of all of its child TextComponents'
         * texts.</p>
         */
        @Override
        public Builder setText(String text) {
            return this;
        } // no function

        /**
         * Adds the provided {@link TextComponent}s to the
         * parent {@link Text} object.
         * 
         * @param components the components to add
         * 
         * @return this Builder
         */
        public Builder addComponents(TextComponent... components) {
            for (TextComponent component : components) {
                this.component.text = this.component.text.concat(component.text);
                component.parent = (Text) this.component;

                Text text = ((Text) this.component);
                if (text.components.size <= 0) {
                    text.components.put(0, component);
                } else {
                    int last = CollectionUtil.lastKey(text.components);
                    int map = last + text.components.get(last).text.length();
                    text.components.put(map, component);
                }
            }

            return this;
        }

        /**
         * Builds the {@link Text} with the assigned
         * parameters and returns it.
         * 
         * <p>The parent Text object must have a
         * {@link Font} assigned to draw with, otherwise
         * this method resolves to an
         * IllegalArgumentException.</p>
         * 
         * @return a new Text object
         * 
         * @throws IllegalArgumentException if the Text
         *         object to build would contain no
         *         components or would not have a Font to
         *         draw with
         */
        public Text build() {
            if (((Text) this.component).components.size <= 0) {
                throw new IllegalArgumentException("Cannot create text with no components");
            }

            if (this.component.font == null) {
                throw new IllegalArgumentException("Text must have a base font");
            }

            if (this.component.color == null) {
                this.component.color = Color.WHITE;
            }

            ((Text) this.component).refreshValues();
            return (Text) this.component;
        }
    }

    private static final DisplayMeta DISPLAY_META;
    static final Logger logger = LoggerFactory.getLogger(Text.class);

    static {
        DISPLAY_META = new DisplayMeta();
    }

    /**
     * Generates a clean {@link DisplayMeta} object to pass
     * to {@link TextStyle}s.
     * 
     * @return a clean DisplayMeta
     */
    static DisplayMeta generateDisplayMeta() {
        DISPLAY_META.reset();
        return DISPLAY_META;
    }

    /**
     * Returns a new {@link Builder} instance to build a new
     * {@link Text}.
     * 
     * @return a Text Builder
     */
    public static Text.Builder builder() {
        return new Text.Builder();
    }

    /**
     * Generates a new {@link Text} object based on the
     * provided parameterized strings.
     * 
     * @param tailor the current Undertailor instance
     * @param baseParams the parameterized string containing
     *        the properties of the base Text
     * @param components the parameterized string containing
     *        all components to add
     * 
     * @return a new Text object
     */
    public static Text of(Undertailor tailor, String baseParams, String components) {
        Builder builder = Text.builder();
        if (baseParams != null && !baseParams.trim().isEmpty()) {
            TextComponent.applyParameters(builder, tailor,
                ParsedText.of(baseParams).getPieces().get(0));
        }

        ParsedText.of(components).getPieces().forEach(piece -> {
            builder.addComponents(TextComponent.of(tailor, piece));
        });

        return builder.build();
    }

    // ---------------- object ----------------

    private int lineCount;
    private Transform transform;
    private Pair<Float> spaceTaken;
    private long instantiationTime;
    private Pair<Integer> stringBounds;
    private OrderedMap<Integer, TextComponent> components; // integer marks start index of the component

    // objects held so we don't spam new objects
    private Pair<Integer> m_valuePair;
    private Transform m_drawnTransform;

    private Text() {
        this.components = new OrderedMap<>();

        this.instantiationTime = TimeUtils.millis();
        this.stringBounds = new Pair<>(0, 0);
        this.spaceTaken = new Pair<>(-1F, -1F);
        this.transform = new Transform();

        this.m_valuePair = new Pair<>();
        this.m_drawnTransform = new Transform();
    }

    // ---------------- abstract method implementation ----------------

    // method-specific variables because stupid scope rules
    float dX, dY, prevSpacing;

    @Override
    public void render(float x, float y) {
        dX = x;
        dY = y;
        prevSpacing = 0;

        this.processCharacters((localIndex, component) -> {
            char character = component.getText().charAt(localIndex.getB());

            if (character == SPACE) { // space?
                dX += font.getSpaceLength() * transform.getScaleX();
                prevSpacing = 0;
            } else if (character == NEWLINE) { // new line?
                dX = x;
                dY -= font.getLineSize() * transform.getScaleY();
                prevSpacing = 0;
            } else { // character?
                Sprite sprite = this.font.getCharacterSprite(character);

                if (sprite != null) { // character actually exists?
                    // grab the stuff we're using
                    Pair<Float> letterSpacing = font.getLetterSpacing(character);
                    // reset the drawing transform
                    this.m_drawnTransform = this.transform.copyInto(this.m_drawnTransform);

                    // process display meta from styles
                    DisplayMeta dMeta = Text.generateDisplayMeta();
                    if (component.getStyles().size > 0) {
                        component.getStyles().forEach(style -> {
                            style.apply(dMeta, TimeUtils.timeSinceMillis(this.instantiationTime),
                                character, localIndex.getA() + localIndex.getB(),
                                this.getText().length());
                        });

                        this.m_drawnTransform
                            .setScaleX(this.m_drawnTransform.getScaleX() * dMeta.scaleX);
                        this.m_drawnTransform
                            .setScaleY(this.m_drawnTransform.getScaleY() * dMeta.scaleY);
                        this.m_drawnTransform.addRotation(dMeta.rotation);
                    }

                    // process character right vs left spacing from previous character
                    if (prevSpacing > 0) {
                        dX += Math.max(prevSpacing, letterSpacing.getA())
                            * m_drawnTransform.getScaleX();
                    }

                    // set our color and final drawing positions
                    font.getRenderer().setBatchColor(component.getColor());
                    float dpX = dX + (dMeta.offX * m_drawnTransform.getScaleX());
                    float dpY = dY + (dMeta.offY * m_drawnTransform.getScaleY());

                    sprite.render(dpX, dpY, m_drawnTransform);

                    // save this character's right-spacing for the next character
                    prevSpacing = letterSpacing.getB();
                    // offset the marker
                    dX += sprite.getTextureRegion().getRegionWidth() * m_drawnTransform.getScaleX();
                }
            }
        });
    }

    // ---------------- getters: immutable/calculated ----------------

    /**
     * Returns the text of this {@link Text} after applying
     * its string bounds.
     * 
     * @return this Text's bounded text
     * 
     * @see #getStringBounds()
     */
    public String getBoundedText() {
        return this.getText().substring(
            this.getStringBounds().getA() == -1 ? 0 : this.getStringBounds().getA(),
            this.getStringBounds().getB() == -1 ? this.getText().length()
                : this.getStringBounds().getB());
    }

    /**
     * Returns all the child {@link TextComponent}
     * components contained within this {@link Text} object.
     * 
     * @return a Collection of this Text's TextComponents
     */
    public OrderedMap.Values<TextComponent> getComponents() {
        return this.components.values();
    }

    /**
     * Returns the space taken by this {@link Text} if it
     * were to be drawn with no transformations. The first
     * value of the pair is the width, the second being the
     * height.
     * 
     * <p>String bounds affect this value.</p>
     * 
     * @return the space taken by this Text with no
     *         transform
     */
    public Pair<Float> getSpaceTaken() {
        return this.spaceTaken;
    }

    /**
     * Returns the count of lines this {@link Text} has
     * <code>(text.trim().split("\n").length)</code>.
     * 
     * <p>String bounds affect this value.</p>
     * 
     * @return the total lines of text this Text object has
     */
    public int getLineCount() {
        return this.lineCount;
    }

    // ---------------- g/s text parameters ----------------

    /**
     * Returns the substring boundaries defining which
     * characters of this {@link Text} object should be
     * considered for drawing.
     * 
     * @return the substring boundaries for this Text
     * 
     * @see #substring(int, int)
     */
    public Pair<Integer> getStringBounds() {
        return this.stringBounds;
    }

    /**
     * Sets the substring boundaries defining which
     * characters of this {@link Text} object should be
     * considered for drawing.
     * 
     * <p>This method is functionally equivalent to
     * substringing the {@link Text} without creating a new
     * instance. If a new instance is needed, see
     * {@link #substring(int, int)}.</p>
     * 
     * @param first the first boundary
     * @param second the second boundary
     * 
     * @see #substring(int, int)
     */
    public void setStringBounds(int first, int second) {
        if (first > second) {
            throw new IllegalArgumentException("First bound cannot be greater than second bound");
        }

        int oFirst = this.stringBounds.getA();
        int oSecond = this.stringBounds.getB();
        this.stringBounds.setItems(first, second);
        if (oFirst != first || oSecond != second) {
            this.refreshValues();
        }
    }

    // ---------------- functional --------------------

    /**
     * Returns the {@link TextComponent} holding the given
     * character at the specified index.
     * 
     * @param index the index of the character in the scope
     *        of the full text
     * 
     * @return the TextComponent owning the character
     */
    public TextComponent getTextComponentAt(int index) {
        if (index < 0 || index >= this.getText().length()) {
            return null;
        }

        for (int i = index + 1; i >= 0; i--) {
            TextComponent comp = this.components.get(i);
            if (comp != null) {
                return comp;
            }
        }

        return null;
    }

    /**
     * Substrings the contents of this {@link Text} object
     * into a new instance.
     * 
     * <p>This method is large, and should not be called
     * every frame. String bounds substitute for this
     * method's functionality should the purpose only be to
     * cut off unneeded characters, though if a new Text
     * instance need to be created then this method suits
     * the use case, however it is advised to generate it in
     * a space that is not called that often.</p>
     * 
     * @param start the first boundary
     * @param end the second boundary
     * 
     * @return a duplicate Text object including only the
     *         characters bounded by the provided indices
     */
    public Text substring(int start, int end) {
        int boundL = start;
        int boundR = end;

        if (boundL < 0) {
            boundL = this.getText().length() - Math.abs(boundL);
        }

        if (boundR < 0) {
            boundR = this.getText().length() - Math.abs(boundR);
        }

        Builder builder = Text.builder();
        builder.copy(this);

        TextComponent first = boundL == 0 ? null : this.getTextComponentAt(boundL);
        TextComponent last = boundR == 0 ? null : this.getTextComponentAt(boundR);

        if (boundL == 0 && boundR == 0) {
            this.components.values().forEach(builder::addComponents);
        } else if (this.components.size == 1) {
            TextComponent.Builder compBuilder = TextComponent.builder();
            TextComponent source = this.components.get(CollectionUtil.firstKey(this.components));
            compBuilder.copy(source);
            compBuilder.setText(source.getText().substring(start, end));
            builder.addComponents(compBuilder.build());
        } else {
            for (OrderedMap.Entry<Integer, TextComponent> entry : this.components.entries()) {
                if (first != null) { // if first is null, we've found the first component to iterate through
                    if (entry.value != first) {
                        continue;
                    }
                }

                if (entry.value == first || entry.value == last) {
                    if (entry.value == first && entry.value == last) {
                        TextComponent.Builder compBuilder = TextComponent.builder();
                        int leftStringBound = boundL - entry.key;

                        compBuilder.copy(entry.value);
                        compBuilder.setText(entry.value.getText().substring(leftStringBound,
                            leftStringBound + (boundR - boundL)));
                        builder.addComponents(compBuilder.build());
                        break;
                    } else {
                        if (entry.value == first) {
                            if (boundL != -1 && entry.key < boundL) {
                                TextComponent.Builder compBuilder = TextComponent.builder();

                                compBuilder.copy(entry.value);
                                compBuilder
                                    .setText(entry.value.getText().substring(boundL - entry.key));
                                builder.addComponents(compBuilder.build());
                            }

                            first = null;
                        }

                        if (entry.value == last && boundR != -1
                            && entry.key + entry.value.text.length() > boundR) {
                            String newText = entry.value.text;
                            TextComponent.Builder compBuilder = TextComponent.builder();

                            newText = newText.substring(0,
                                newText.length() - (newText.length() + entry.key - boundR));
                            compBuilder.copy(entry.value);
                            compBuilder.setText(newText);
                            builder.addComponents(compBuilder.build());
                            break;
                        }
                    }
                } else {
                    builder.addComponents(entry.value);
                }
            }
        }

        return builder.build();
    }

    // ---------------- internal methods ----------------

    /**
     * Internal method.
     * 
     * <p>Responsible for DRYing the process of iterating
     * through each character within the string bounds.</p>
     * 
     * <p>The consumer takes a pair of integers and a
     * TextComponent. The first integer is the index of the
     * component's first character in the entire text. The
     * second integer is the index of the current character,
     * local to the scope of the current TextComponent.</p>
     * 
     * <p>Assume a component with the text content <code>
     * "Hello, world!"</code>. To point to the character
     * <code>o</code> at index 4, the consumer is provided
     * with 4 and the component holding the text
     * <code>(component.getText().charAt(4) =
     * 'o')</code></p>
     * 
     * @param consumer the consumer that processes each
     *        character
     */
    private void processCharacters(BiConsumer<Pair<Integer>, TextComponent> consumer) {
        int boundL = this.getStringBounds().getA();
        int boundR = this.getStringBounds().getB();

        if (boundL < 0) {
            boundL = this.getText().length() - Math.abs(boundL);
        }

        if (boundR < 0) {
            boundR = this.getText().length() - Math.abs(boundR);
        }

        TextComponent first = boundL == 0 ? null : this.getTextComponentAt(boundL);
        TextComponent last = boundR == 0 ? null : this.getTextComponentAt(boundR);
        for (OrderedMap.Entry<Integer, TextComponent> entry : this.components.entries()) {
            if (first != null) { // if first is null, we've found the first component to iterate through
                if (entry.value != first) {
                    continue;
                }

                first = null;
            }

            int localIndex = 0;
            if (boundL != 0 && entry.key < boundL) {
                localIndex += boundL - entry.key;
            }

            this.m_valuePair.setA(entry.key);
            for (int ind = localIndex; ind < entry.value.getText().length(); ind++) {
                this.m_valuePair.setB(ind);
                consumer.accept(this.m_valuePair, entry.value);

                if (boundR != 0 && entry.key + ind >= boundR) {
                    return;
                }
            }

            if (entry.value == last) {
                return;
            }
        }
    }

    /**
     * Internal method.
     * 
     * <p>Convenience method refreshing all pre-calculated
     * values.</p>
     */
    private void refreshValues() {
        this.calculateSpace();
        this.calculateLines();
    }

    // method-specific variables cuz stupid scope rules
    // float dX, dY; (reused)

    /**
     * Internal method.
     * 
     * <p>Calculates the value returned by
     * {@link #getSpaceTaken()}.</p>
     */
    private void calculateSpace() {
        this.spaceTaken.setItems(0F, 0F);
        dX = 0;
        dY = font.getLineSize();
        prevSpacing = 0;

        this.processCharacters((localIndex, component) -> {
            char character = component.getText().charAt(localIndex.getB());

            if (character == ' ') { // space?
                dX += font.getSpaceLength() * transform.getScaleX();
                prevSpacing = 0;
            } else if (character == '\n') { // new line?
                if (dX > this.spaceTaken.getA()) {
                    this.spaceTaken.setA(dX);
                }

                dX = 0;
                dY -= font.getLineSize() * transform.getScaleY();
                prevSpacing = 0;
            } else { // character?
                Sprite sprite = this.font.getCharacterSprite(character);

                if (sprite != null) { // character actually exists?
                    // grab the stuff we're using
                    Pair<Float> letterSpacing = font.getLetterSpacing(character);

                    // process character right vs left spacing from previous character
                    if (prevSpacing > 0) {
                        dX += Math.max(prevSpacing, letterSpacing.getA());
                    }

                    // save this character's right-spacing for the next character
                    prevSpacing = letterSpacing.getB();
                    // offset the marker
                    dX += sprite.getTextureRegion().getRegionWidth();
                }
            }
        });

        if (dX > this.spaceTaken.getA()) {
            this.spaceTaken.setA(dX);
        }

        this.spaceTaken.setB(dY);
    }

    /**
     * Internal method.
     * 
     * <p>Calculates the value returned by
     * {@link #getLineCount()}.</p>
     */
    private void calculateLines() {
        if (this.getText().trim().isEmpty()) {
            this.lineCount = 0;
        } else {
            this.lineCount = this.getBoundedText().trim().split("\n").length;
        }
    }
}
