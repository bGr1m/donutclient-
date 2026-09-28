package dev.donut.client.config;

public final class ClientConfig {
    public int schemaVersion = 1;
    public FinderSettings storage = new FinderSettings();
    public FinderSettings spawner = spawnerDefaults();
    public FreecamSettings freecam = new FreecamSettings();
    public PearlSaveSettings pearlSave = new PearlSaveSettings();
    public MlgSettings mlg = new MlgSettings();
    public RtpStashSettings rtpStash = new RtpStashSettings();
    public AutoEatSettings autoEat = new AutoEatSettings();
    public AutoToolSettings autoTool = new AutoToolSettings();
    public RtpPlusSettings rtpPlus = new RtpPlusSettings();
    public BlockEspSettings blockEsp = new BlockEspSettings();
    public int accentColor = 0x4C86FF;
    public boolean showHud = true;

    private static FinderSettings spawnerDefaults() {
        FinderSettings settings = new FinderSettings();
        settings.color = 0xFF779C;
        settings.perTypeColors = false;
        return settings;
    }

    public void sanitize() {
        if (storage == null) storage = new FinderSettings();
        if (spawner == null) spawner = spawnerDefaults();
        if (freecam == null) freecam = new FreecamSettings();
        if (pearlSave == null) pearlSave = new PearlSaveSettings();
        if (mlg == null) mlg = new MlgSettings();
        if (rtpStash == null) rtpStash = new RtpStashSettings();
        if (autoEat == null) autoEat = new AutoEatSettings();
        if (autoTool == null) autoTool = new AutoToolSettings();
        if (rtpPlus == null) rtpPlus = new RtpPlusSettings();
        if (blockEsp == null) blockEsp = new BlockEspSettings();
        storage.sanitize();
        spawner.sanitize();
        freecam.sanitize();
        pearlSave.sanitize();
        mlg.sanitize();
        rtpStash.sanitize();
        autoEat.sanitize();
        autoTool.sanitize();
        rtpPlus.sanitize();
        blockEsp.sanitize();
        accentColor &= 0xFFFFFF;
    }
}
