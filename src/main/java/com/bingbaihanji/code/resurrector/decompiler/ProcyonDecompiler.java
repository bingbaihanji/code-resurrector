package com.bingbaihanji.code.resurrector.decompiler;

import com.bingbaihanji.code.resurrector.ConfigSaver;
import com.bingbaihanji.code.resurrector.LuytenTypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;

import java.io.StringWriter;

/**
 * Procyon反编译引擎实现
 */
public class ProcyonDecompiler implements IDecompiler {

    private DecompilerSettings settings;
    private DecompilationOptions options;
    private MetadataSystem metadataSystem;
    private LuytenTypeLoader typeLoader;

    public ProcyonDecompiler() {
        initialize();
    }

    @Override
    public void initialize() {
        ConfigSaver configSaver = ConfigSaver.getLoadedInstance();
        settings = configSaver.getDecompilerSettings();

        typeLoader = new LuytenTypeLoader();
        metadataSystem = new MetadataSystem(typeLoader);

        options = new DecompilationOptions();
        options.setSettings(settings);
        options.setFullDecompilation(true);
    }

    @Override
    public String decompile(String classFilePath, byte[] classBytes) throws Exception {
        // Procyon使用内部名称 (例如: java/lang/String)
        String internalName = classFilePath.replace(".class", "");
        return decompileType(internalName, classBytes);
    }

    @Override
    public String decompileType(String typeName, byte[] classBytes) throws Exception {
        TypeReference typeRef = metadataSystem.lookupType(typeName);
        if (typeRef == null) {
            throw new Exception("无法找到类型: " + typeName);
        }

        TypeDefinition typeDef = typeRef.resolve();
        if (typeDef == null) {
            throw new Exception("无法解析类型: " + typeName);
        }

        StringWriter writer = new StringWriter();
        PlainTextOutput output = new PlainTextOutput(writer);
        output.setUnicodeOutputEnabled(settings.isUnicodeOutputEnabled());

        settings.getLanguage().decompileType(typeDef, output, options);

        return writer.toString();
    }

    @Override
    public DecompilerType getType() {
        return DecompilerType.PROCYON;
    }

    @Override
    public void cleanup() {
        // Procyon资源清理
        if (typeLoader != null) {
            typeLoader.getTypeLoaders().clear();
        }
    }

    public DecompilerSettings getSettings() {
        return settings;
    }

    public MetadataSystem getMetadataSystem() {
        return metadataSystem;
    }

    public LuytenTypeLoader getTypeLoader() {
        return typeLoader;
    }

    public DecompilationOptions getOptions() {
        return options;
    }
}
