package net.machinemuse.powersuits.client.render.modelspec;

import com.google.common.collect.ImmutableMap;
import net.machinemuse.numina.math.geometry.Colour;
import net.machinemuse.numina.utils.MuseLogger;
import net.machinemuse.numina.utils.string.MuseStringUtils;
import net.machinemuse.powersuits.client.model.obj.OBJModelPlus;
import net.machinemuse.powersuits.common.config.MPSConfig;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.SimpleModelState;
import net.minecraftforge.common.model.IModelPart;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nullable;
import javax.vecmath.Vector3f;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


/**
 * Author: MachineMuse (Claire Semple)
 * Created: 8:44 AM, 4/28/13
 *
 * Ported to Java by lehjr on 11/8/16.
 */
@SideOnly(Side.CLIENT)
public class ModelSpecXMLReader {
    private static ModelSpecXMLReader INSTANCE;
    public static ModelSpecXMLReader getInstance() {
        if (INSTANCE == null) {
            synchronized (ModelSpecXMLReader.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ModelSpecXMLReader();
                }
            }
        }
        return INSTANCE;
    }

    private ModelSpecXMLReader() {
    }

    public static void parseFile(File file, @Nullable TextureStitchEvent event) {
        String filePath = file.getAbsoluteFile().getAbsolutePath();
        if (file.exists()) {
            try {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document xml = dBuilder.parse(file);
                xml.normalizeDocument();

                if (xml.hasChildNodes()) {
                    NodeList specList = xml.getElementsByTagName("modelspec");
                    for(int i = 0; i< specList.getLength(); i++) {
                        Node specNode = specList.item(i);
                        if(specNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element eElement = (Element) specNode;
                            EnumSpecType specType = EnumSpecType.getTypeFromName(eElement.getAttribute("type"));
                            String specName = eElement.getAttribute("specName");

                            boolean isDefault = (eElement.hasAttribute("default") ? Boolean.parseBoolean(eElement.getAttribute("default")) : false);

                            switch(specType) {
                                case POWER_FIST:
                                    // only allow custom models if allowed by config
                                    if (isDefault || MPSConfig.getInstance().allowCustomPowerFistModels())
                                        parseModelSpec(specNode, event, EnumSpecType.POWER_FIST, specName, isDefault);
                                    break;

                                case ARMOR_MODEL:
                                    // only allow these models if allowed by config
                                    if (MPSConfig.getInstance().allowHighPollyArmorModels()) {
                                        // only allow custom models if allowed by config
                                        if (isDefault || MPSConfig.getInstance().allowCustomHighPollyArmor())
                                            parseModelSpec(specNode, event, EnumSpecType.ARMOR_MODEL, specName, isDefault);
                                    }
                                    break;

                                case ARMOR_SKIN:
                                    if (event == null) {
                                        TextureSpec textureSpec = new TextureSpec(specName, isDefault);
                                        parseTextureSpec(specNode,textureSpec);
                                    }
                                    break;

                                default:
                                    break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                MuseLogger.logException(new StringBuilder("Failed to load ModelSpecFile: ").append(filePath).append(": ").toString(), e);
            }
        }
    }

    public static void parseTextureSpec(Node specNode, TextureSpec textureSpec) {
        // ModelBase textures are not registered.
        TextureSpec existingspec = (TextureSpec) ModelRegistry.getInstance().put(textureSpec.getName(), textureSpec);
        NodeList textures = specNode.getOwnerDocument().getElementsByTagName("texture");
        for (int i = 0; i < textures.getLength(); i++) {
            Node textureNode = textures.item(i);
            if (textureNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element)textureNode;
                String fileLocation = eElement.getAttribute("file");
                NodeList bindings = eElement.getElementsByTagName("binding");
                for (int j = 0; j < bindings.getLength(); j++) {
                    Binding binding = getBinding(bindings.item(j));
                    getTexturePartSpec(existingspec, bindings.item(j), binding.getSlot(), fileLocation);
                }
            }
        }
    }

    /**
     * Biggest difference between the ModelSpec for Armor vs PowerFist is that the armor models don't need item camera transforms
     */
    public static void parseModelSpec(Node specNode, TextureStitchEvent event, EnumSpecType specType, String specName, boolean isDefault) {
        NodeList models = specNode.getOwnerDocument().getElementsByTagName("model");
        List<String> textures = new ArrayList<>();
        IModelState modelState = null;

        for (int i=0; i < models.getLength(); i++) {

            Node modelNode = models.item(i);
            if (modelNode.getNodeType() == Node.ELEMENT_NODE) {
                Element modelElement = (Element)modelNode;

                // Register textures
                if (event != null) {
                    List<String> tempTextures = Arrays.asList(modelElement.getAttribute("textures").split(","));
                    for (String texture: tempTextures)
                        if (!(textures.contains(texture)))
                            textures.add(texture);
                } else {
                    String modelLocation = modelElement.getAttribute("file");
                    // IModelStates should be per model, not per spec
                    NodeList cameraTransformList = modelElement.getElementsByTagName("itemCameraTransforms");
                    // check for item camera transforms, then fall back on single transform for model
                    if (cameraTransformList.getLength() > 0) {
                        Node cameraTransformNode = cameraTransformList.item(0);
                        modelState = getIModelState(cameraTransformNode);
                    } else {
                        // Get the transform for the model and add to the registry
                        NodeList transformNodeList = modelElement.getElementsByTagName("trsrTransformation");
                        if (transformNodeList.getLength() > 0)
                            modelState = getTransform(transformNodeList.item(0));
                        else
                            modelState= TRSRTransformation.identity();
                    }

                    OBJModelPlus.OBJBakedModelPus bakedModel = ModelRegistry.getInstance().loadBakedModel(new ResourceLocation(modelLocation), modelState);
                    // ModelSpec stuff
                    if (bakedModel != null && bakedModel instanceof OBJModelPlus.OBJBakedModelPus) {
                        ModelSpec modelspec = new ModelSpec(bakedModel, modelState, specName, isDefault, specType);
                        ModelSpec existingspec = (ModelSpec) ModelRegistry.getInstance().put(MuseStringUtils.extractName(modelLocation), modelspec);
                        NodeList bindingNodeList = ((Element) modelNode).getElementsByTagName("binding");
                        if (bindingNodeList.getLength() > 0) {
                            for (int k = 0; k < bindingNodeList.getLength(); k++) {
                                Node bindingNode = bindingNodeList.item(k);
                                Binding binding = getBinding(bindingNode);
                                NodeList partNodeList = ((Element) bindingNode).getElementsByTagName("part");
                                for(int j=0; j<partNodeList.getLength(); j++) {
                                    getModelPartSpec(existingspec, partNodeList.item(j), binding);
                                }
                            }
                        }
                    } else {
                        MuseLogger.logError("Model file " + modelLocation + " not found! D:");
                    }
                }
            }
        }

        // Register textures
        if (event != null)
            for (String texture : textures)
                event.getMap().registerSprite(new ResourceLocation(texture));
    }

    // since the skinned armor can't have more than one texture per EntityEquipmentSlot the TexturePartSpec is named after the slot
    public static void getTexturePartSpec(TextureSpec textureSpec, Node bindingNode, EntityEquipmentSlot slot, String fileLocation) {
        Element partSpecElement = (Element) bindingNode;
        Colour colour = parseColour(partSpecElement.getAttribute("defaultcolor"));

        if (!Objects.equals(slot, null) && Objects.equals(slot.getSlotType(), EntityEquipmentSlot.Type.ARMOR))
            textureSpec.put(slot.getName(),
                    new TexturePartSpec(textureSpec,
                            new Binding(null, slot, "all"),
                            colour.getInt(), slot.getName(), fileLocation));
    }

    /**
     * ModelPartSpec is a group of settings for each model part
     */
    public static void getModelPartSpec(ModelSpec modelSpec, Node partSpecNode, Binding binding) {
        Element partSpecElement = (Element) partSpecNode;
        String partname = validatePolygroup(partSpecElement.getAttribute("partName"), modelSpec);
        boolean glow = Boolean.parseBoolean(partSpecElement.getAttribute("defaultglow"));
        Colour colour = parseColour(partSpecElement.getAttribute("defaultcolor"));
        if (partname == null) {
            System.out.println("partName is NULL!!");
            System.out.println("ModelSpec model: " + modelSpec.getName());
            System.out.println("glow: " + glow);
            System.out.println("colour: " + colour.hexColour());
        } else
            modelSpec.put(partname, new ModelPartSpec(modelSpec,
                    binding,
                    partname,
                    colour.getInt(),
                    glow));
    }

    @Nullable
    public static String validatePolygroup(String s, ModelSpec m) {
        return m.getModel().getModel().getMatLib().getGroups().keySet().contains(s) ? s : null;
    }

    /**
     * This gets the map of TransformType, TRSRTransformation> used for handheld items
     * @param itemCameraTransformsNode
     * @return
     */
    public static IModelState getIModelState(Node itemCameraTransformsNode) {
        ImmutableMap.Builder<IModelPart, TRSRTransformation> builder = ImmutableMap.builder();
        NodeList transformationList = ((Element)itemCameraTransformsNode).getElementsByTagName("trsrTransformation");
        for (int i = 0; i < transformationList.getLength(); i++) {
            Node transformationNode = transformationList.item(i);
            ItemCameraTransforms.TransformType transformType =
                    ItemCameraTransforms.TransformType.valueOf(((Element) transformationNode).getAttribute("type").toUpperCase());
            TRSRTransformation trsrTransformation = getTransform(transformationNode);
            builder.put(transformType, trsrTransformation);
        }
        return new SimpleModelState(builder.build());
    }

    /**
     * This gets the transforms for baking the models. TRSRTransformation is also used for item camera transforms to alter the
     * position, scale, and translation of a held/dropped/framed item
     *
     * @param transformationNode
     * @return
     */
    public static TRSRTransformation getTransform(Node transformationNode) {
        Vector3f translation = parseVector(((Element) transformationNode).getAttribute("translation"));
        Vector3f rotation = parseVector(((Element) transformationNode).getAttribute("rotation"));
        Vector3f scale = parseVector(((Element) transformationNode).getAttribute("scale"));
        return getTransform(translation, rotation, scale);
    }

    /**
     * Binding is a subset if settings for the ModelPartSpec
     */
    public static Binding getBinding(Node bindingNode) {
        return new Binding(
                (((Element) bindingNode).hasAttribute("target")) ?
                        MorphTarget.getMorph(((Element) bindingNode).getAttribute("target")) : null,
                (((Element) bindingNode).hasAttribute("slot")) ?
                        EntityEquipmentSlot.fromString(((Element) bindingNode).getAttribute("slot").toLowerCase()) : null,
                (((Element) bindingNode).hasAttribute("itemState")) ?
                        ((Element) bindingNode).getAttribute("itemState"): "all"
        );
    }

    /**
     * Simple transformation for armor models. Powerfist (and shield?) will need one of these for every conceivable case except GUI which will be an icon
     */
    public static TRSRTransformation getTransform(@Nullable Vector3f translation, @Nullable Vector3f rotation, @Nullable Vector3f scale) {
        if (translation == null)
            translation = new Vector3f(0, 0, 0);
        if (rotation == null)
            rotation = new Vector3f(0, 0, 0);
        if (scale == null)
            scale = new Vector3f(1, 1, 1);

        return new TRSRTransformation(
                // Transform
                new Vector3f(translation.x / 16, translation.y / 16, translation.z / 16),
                // Angles
                TRSRTransformation.quatFromXYZDegrees(rotation),
                // Scale
                scale,
                null);
    }

    @Nullable
    public static Vector3f parseVector(String s) {
        try {
            String[] ss = s.split(",");
            float x = Float.parseFloat(ss[0]);
            float y = Float.parseFloat(ss[1]);
            float z = Float.parseFloat(ss[2]);
            return new Vector3f(x, y, z);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static Colour parseColour(String s) {
        try {
            int value = Integer.parseInt(s, 16);
            Color c = new Color(value);
            return new Colour(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
        } catch (Exception e){
            return Colour.WHITE;
        }
    }
}