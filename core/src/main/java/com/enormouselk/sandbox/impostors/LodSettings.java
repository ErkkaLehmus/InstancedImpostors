package com.enormouselk.sandbox.impostors;

public class LodSettings {
    public static final int SHADERTYPE_MINIMAL = 0;
    public static final int SHADERTYPE_DEFAULT = 1;
    public static final int SHADERTYPE_PBR = 2;

    String filename;
    String ID;
    int lodMax;
    boolean generateImpostor;
    int shaderType;
    boolean external;

    public LodSettings(String filename, String ID, int lodMax, boolean generateImpostor, int shaderType, boolean external) {
        this.filename = filename;
        this.ID = ID;
        this.lodMax = lodMax;
        this.generateImpostor = generateImpostor;
        this.shaderType = shaderType;
        this.external = external;
    }
}
