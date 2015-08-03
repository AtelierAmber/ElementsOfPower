package gigaherz.elementsofpower.models;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import gigaherz.elementsofpower.ElementsOfPower;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.*;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidParameterException;
import java.util.*;

public class ObjModel implements IModel
{
    public List<Vector3f> positions;
    public List<Vector3f> normals;
    public List<Vector2f> texCoords;

    public final List<MeshPart> parts = new ArrayList<>();

    final Map<String, ResourceLocation> textures = new HashMap<>();
    private final Set<ResourceLocation> usedTextures = new HashSet<>();

    ModelBlock modelBlock;

    @Override
    public IFlexibleBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
    {
        TextureAtlasSprite particle = bakedTextureGetter.apply(textures.get("particle"));

        ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();

        for (MeshPart part : parts)
        {
            TextureAtlasSprite sprite = null;
            int color = 0xFFFFFFFF;

            Material m = part.material;
            if (m == null)
            {
                if (part.materialName != null)
                {
                    sprite = bakedTextureGetter.apply(textures.get(part.materialName));
                }
            }
            else
            {
                if (m.DiffuseTextureMap != null)
                {
                    sprite = bakedTextureGetter.apply(textures.get(m.DiffuseTextureMap));
                }
                else if (m.AmbientTextureMap != null)
                {
                    sprite = bakedTextureGetter.apply(textures.get(m.AmbientTextureMap));
                }

                if (m.DiffuseColor != null)
                {
                    int r = (int) m.DiffuseColor.x;
                    int g = (int) m.DiffuseColor.y;
                    int b = (int) m.DiffuseColor.z;
                    color = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
            }

            for (int[][] face : part.indices)
            {
                int[] faceData = new int[28];

                processVertex(faceData, 0, face[0], color, sprite);
                processVertex(faceData, 1, face[1], color, sprite);
                processVertex(faceData, 2, face[2], color, sprite);
                processVertex(faceData, 3, face[3], color, sprite);

                builder.add(new BakedQuad(faceData, -1, FaceBakery.getFacingFromVertexData(faceData)));
            }
        }

        return new ObjModelLoader.BakedModel(builder.build(), modelBlock, particle, getVertexFormat());
    }

    private void processVertex(int[] faceData, int i, int[] vertex, int color, TextureAtlasSprite sprite)
    {
        Vector3f position = new Vector3f(0, 0, 0);
        Vector2f texCoord = new Vector2f(0, 0);

        int p = 0;

        if (positions != null)
            position = positions.get(vertex[p++]);

        if (normals != null)
            p++; // normals not used by minecraft

        if (texCoords != null)
        {
            texCoord = texCoords.get(vertex[p]);

            if (sprite != null)
            {
                texCoord = new Vector2f(
                        sprite.getInterpolatedU(texCoord.x * 16),
                        sprite.getInterpolatedV(texCoord.y * 16));
            }
        }

        int l = i * 7;
        faceData[l++] = Float.floatToRawIntBits(position.x);
        faceData[l++] = Float.floatToRawIntBits(position.y);
        faceData[l++] = Float.floatToRawIntBits(position.z);
        faceData[l++] = color;
        faceData[l++] = Float.floatToRawIntBits(texCoord.x);
        faceData[l++] = Float.floatToRawIntBits(texCoord.y);
        faceData[l] = 0;
    }

    public VertexFormat getVertexFormat()
    {
        return Attributes.DEFAULT_BAKED_FORMAT;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<ResourceLocation> getTextures()
    {
        ModelBlock modelblock = modelBlock;

        while (modelblock != null)
        {
            for (Map.Entry<String, String> e : ((Map<String, String>) modelblock.textures).entrySet())
            {
                if (!textures.containsKey(e.getKey()))
                    textures.put(e.getKey(), new ResourceLocation(e.getValue()));
            }
            modelblock = modelblock.parent;
        }

        for (MeshPart p : parts)
        {
            Material m = p.material;

            if (m == null)
            {
                if (p.materialName != null)
                {
                    ResourceLocation s = textures.get(p.materialName);
                    if (s != null)
                        usedTextures.add(s);
                }
                continue;
            }

            if (m.DiffuseTextureMap != null)
            {
                ResourceLocation s = textures.get(m.DiffuseTextureMap);
                if (s != null)
                    usedTextures.add(s);
            }
            else if (m.AmbientTextureMap != null)
            {
                ResourceLocation s = textures.get(m.AmbientTextureMap);
                if (s != null)
                    usedTextures.add(s);
            }
        }

        if (textures.containsKey("particle"))
            usedTextures.add(textures.get("particle"));

        return usedTextures;

    }

    @Override
    public Collection<ResourceLocation> getDependencies()
    {
        // I guess this could be used to load the .json files,
        // but I have no idea how I'd obtain them later, so...
        return Collections.emptyList();
    }

    @Override
    public IModelState getDefaultState()
    {
        return null;
    }

    public static ObjModel loadFromResource(ResourceLocation modelLocation) throws IOException
    {
        return new Reader(modelLocation).loadFromResource();
    }

    public static class MeshPart
    {
        public String materialName;
        public Material material;
        public List<int[][]> indices;

        public MeshPart()
        {
            indices = new ArrayList<>();
        }

        public void addFace(int[]... f)
        {
            indices.add(f);
        }
    }

    public static class Reader
    {
        static final Set<String> unknownCommands = new HashSet<String>();

        private ObjModel currentModel;
        private MaterialLibrary currentMatLib;

        private ObjModel.MeshPart currentPart;

        private final ResourceLocation modelLocation;

        private Reader(ResourceLocation modelLocation)
        {
            this.modelLocation = modelLocation;
        }

        public void reset()
        {
            currentModel = null;
            currentMatLib = null;
        }

        private void addTexCoord(String line)
        {
            String[] args = line.split(" ");

            float x = Float.parseFloat(args[0]);
            float y = Float.parseFloat(args[1]);

            if (currentModel.texCoords == null)
                currentModel.texCoords = new ArrayList<>();
            currentModel.texCoords.add(new Vector2f(x, y));
        }

        private void addNormal(String line)
        {
            String[] args = line.split(" ");

            float x = Float.parseFloat(args[0]);
            float y = Float.parseFloat(args[1]);

            float z = args[2].equals("\\\\")
                    ? (float) Math.sqrt(1 - x * x - y * y)
                    : Float.parseFloat(args[2]);

            if (currentModel.normals == null)
                currentModel.normals = new ArrayList<>();
            currentModel.normals.add(new Vector3f(x, y, z));
        }

        private void addPosition(String line)
        {
            String[] args = line.split(" ");

            float x = Float.parseFloat(args[0]);
            float y = Float.parseFloat(args[1]);
            float z = Float.parseFloat(args[2]);

            if (currentModel.positions == null)
                currentModel.positions = new ArrayList<>();
            currentModel.positions.add(new Vector3f(x, y, z));
        }

        private void addFace(String line)
        {
            String[] args = line.split(" ");

            if (args.length < 3 || args.length > 4)
                throw new InvalidParameterException();

            String[] p1 = args[0].split("/");
            String[] p2 = args[1].split("/");
            String[] p3 = args[2].split("/");

            int[] v1 = parseIndices(p1);
            int[] v2 = parseIndices(p2);
            int[] v3 = parseIndices(p3);

            if (args.length == 3)
            {
                currentPart.addFace(v1, v2, v3, v3);
            }
            else if (args.length == 4)
            {
                String[] p4 = args[3].split("/");
                int[] v4 = parseIndices(p4);

                currentPart.addFace(v1, v2, v3, v4);
            }
        }

        private int[] parseIndices(String[] p1)
        {
            int[] indices = new int[p1.length];
            for (int i = 0; i < p1.length; i++)
            {
                indices[i] = Integer.parseInt(p1[i]) - 1;
            }
            return indices;
        }

        private void useMaterial(String matName)
        {
            currentPart = new ObjModel.MeshPart();
            currentPart.materialName = matName;
            currentPart.material = currentMatLib.materials.get(matName);
            currentModel.parts.add(currentPart);
        }

        private void newObject(String line)
        {
        }

        private void newGroup(String line)
        {
        }

        private void loadMaterialLibrary(ResourceLocation locOfParent, String path) throws IOException
        {
            String prefix = locOfParent.getResourcePath();
            int pp = prefix.lastIndexOf('/');
            prefix = (pp >= 0) ? prefix.substring(0, pp + 1) : "";

            ResourceLocation loc = new ResourceLocation(locOfParent.getResourceDomain(), prefix + path);

            currentMatLib.loadFromStream(loc);
        }

        private ObjModel loadFromResource() throws IOException
        {
            currentModel = new ObjModel();
            currentMatLib = new MaterialLibrary();

            IResource res = Minecraft.getMinecraft().getResourceManager().getResource(modelLocation);
            InputStreamReader lineStream = new InputStreamReader(res.getInputStream(), Charsets.UTF_8);
            BufferedReader lineReader = new BufferedReader(lineStream);

            for (; ; )
            {
                String currentLine = lineReader.readLine();
                if (currentLine == null)
                    break;

                if (currentLine.length() == 0 || currentLine.startsWith("#"))
                {
                    continue;
                }

                String[] fields = currentLine.split(" ", 2);
                String keyword = fields[0];
                String data = fields[1];

                if (keyword.equalsIgnoreCase("o"))
                {
                    newObject(data);
                }
                else if (keyword.equalsIgnoreCase("g"))
                {
                    newGroup(data);
                }
                else if (keyword.equalsIgnoreCase("mtllib"))
                {
                    loadMaterialLibrary(modelLocation, data);
                }
                else if (keyword.equalsIgnoreCase("usemtl"))
                {
                    useMaterial(data);
                }
                else if (keyword.equalsIgnoreCase("v"))
                {
                    addPosition(data);
                }
                else if (keyword.equalsIgnoreCase("vn"))
                {
                    addNormal(data);
                }
                else if (keyword.equalsIgnoreCase("vt"))
                {
                    addTexCoord(data);
                }
                else if (keyword.equalsIgnoreCase("f"))
                {
                    addFace(data);
                }
                else
                {
                    if (!unknownCommands.contains(keyword))
                    {
                        ElementsOfPower.logger.warn("Unrecognized command: " + currentLine);
                        unknownCommands.add(keyword);
                    }
                }
            }

            currentPart = null;
            return currentModel;
        }
    }

    static class Material
    {
        public Vector3f AmbientColor;
        public Vector3f DiffuseColor;
        public Vector3f SpecularColor;
        public float SpecularCoefficient;

        public float Transparency;

        public int IlluminationModel;

        public String AmbientTextureMap;
        public String DiffuseTextureMap;

        public String SpecularTextureMap;
        public String SpecularHighlightTextureMap;

        public String BumpMap;
        public String DisplacementMap;
        public String StencilDecalMap;

        public String AlphaTextureMap;
    }

    static class MaterialLibrary
    {
        static final Set<String> unknownCommands = new HashSet<>();

        public final Dictionary<String, Material> materials = new Hashtable<>();

        public void loadFromStream(ResourceLocation loc) throws IOException
        {
            IResource res = Minecraft.getMinecraft().getResourceManager().getResource(loc);
            InputStreamReader lineStream = new InputStreamReader(res.getInputStream(), Charsets.UTF_8);
            BufferedReader lineReader = new BufferedReader(lineStream);

            Material currentMaterial = null;
            for (; ; )
            {
                String currentLine = lineReader.readLine();
                if (currentLine == null)
                    break;

                if (currentLine.length() == 0 || currentLine.startsWith("#"))
                    continue;

                String[] fields = currentLine.split(" ", 2);
                String keyword = fields[0];
                String data = fields[1];

                if (keyword.equalsIgnoreCase("newmtl"))
                {
                    currentMaterial = new Material();
                    materials.put(data, currentMaterial);
                }
                else if (currentMaterial == null)
                {
                    throw new IOException("Found material attributes before 'newmtl'");
                }
                else if (keyword.equalsIgnoreCase("Ka"))
                {
                    currentMaterial.AmbientColor = parseVector3f(data);
                }
                else if (keyword.equalsIgnoreCase("Kd"))
                {
                    currentMaterial.DiffuseColor = parseVector3f(data);
                }
                else if (keyword.equalsIgnoreCase("Ks"))
                {
                    currentMaterial.SpecularColor = parseVector3f(data);
                }
                else if (keyword.equalsIgnoreCase("Ns"))
                {
                    currentMaterial.SpecularCoefficient = Float.parseFloat(data);
                }
                else if (keyword.equalsIgnoreCase("Tr") ||
                        keyword.equalsIgnoreCase("d"))
                {
                    currentMaterial.Transparency = Float.parseFloat(data);
                }
                else if (keyword.equalsIgnoreCase("illum"))
                {
                    currentMaterial.IlluminationModel = Integer.parseInt(data);
                }
                else if (keyword.equalsIgnoreCase("map_Ka"))
                {
                    currentMaterial.AmbientTextureMap = data;
                }
                else if (keyword.equalsIgnoreCase("map_Kd"))
                {
                    currentMaterial.DiffuseTextureMap = data;
                }
                else if (keyword.equalsIgnoreCase("map_Ks"))
                {
                    currentMaterial.SpecularTextureMap = data;
                }
                else if (keyword.equalsIgnoreCase("map_Ns"))
                {
                    currentMaterial.SpecularHighlightTextureMap = data;
                }
                else if (keyword.equalsIgnoreCase("map_d"))
                {
                    currentMaterial.AlphaTextureMap = data;
                }
                else if (keyword.equalsIgnoreCase("map_bump") ||
                        keyword.equalsIgnoreCase("bump"))
                {
                    currentMaterial.BumpMap = data;
                }
                else if (keyword.equalsIgnoreCase("disp"))
                {
                    currentMaterial.DisplacementMap = data;
                }
                else if (keyword.equalsIgnoreCase("decal"))
                {
                    currentMaterial.StencilDecalMap = data;
                }
                else
                {
                    if (!unknownCommands.contains(keyword))
                    {
                        ElementsOfPower.logger.info("Unrecognized command: " + currentLine);
                        unknownCommands.add(keyword);
                    }
                }
            }
        }

        static Vector3f parseVector3f(String data)
        {
            String[] parts = data.split(" ");
            return new Vector3f(
                    Float.parseFloat(parts[0]),
                    Float.parseFloat(parts[1]),
                    Float.parseFloat(parts[2])
            );
        }
    }
}
