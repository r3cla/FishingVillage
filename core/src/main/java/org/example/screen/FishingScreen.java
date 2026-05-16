package org.example.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;

import org.example.Fonts;
import org.example.FishingVillageGame;
import org.example.fish.CatchResult;
import org.example.fish.Fish;
import org.example.fish.FishRegistry;
import org.example.fish.Rarity;
import org.example.time.GameClock;
import org.example.time.TimeOfDay;
import org.example.weather.Weather;
import org.example.weather.WeatherSystem;

import org.example.game.Achievement;
import org.example.game.CaughtFish;
import org.example.game.FishBag;
import org.example.game.FishRecord;
import org.example.game.JunkType;
import org.example.gear.BaitType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FishingScreen extends ScreenAdapter {

    private enum FishingState { IDLE, CASTING, WAITING, BITING, MINIGAME, REELING, SHOW_RESULT }

    private static final float W       = 1280f;
    private static final float H       = 720f;
    private static final float hz = 320f;

    private static final float FISHER_X  = 630f;
    private static final float ROD_SCALE = 1.7f;

    // tune ARM_Y_STANDING / ARM_Y_SITTING independently to align rod sprite
    private static final float ARM_X_OFFSET   =  65f;
    private static final float ARM_Y_STANDING = 135f;
    private static final float ARM_Y_SITTING  = 145f;

    private float armX() { return FISHER_X + ARM_X_OFFSET; }
    private float armY() {
        boolean s = fishingState == FishingState.IDLE || fishingState == FishingState.SHOW_RESULT;
        return s ? hz - 92f + ARM_Y_STANDING : hz - 108f + ARM_Y_SITTING;
    }
    // rod tip in world space, derived from sprite pixel offsets at ROD_SCALE
    // handle pixel: (20,75), tip pixel: (143,18) in the 160x100 rod image
    private float rodTipX() { return armX() + 123f * ROD_SCALE; }  // armX + 246
    private float rodTipY() { return armY() +  57f * ROD_SCALE; }  // armY + 114
    private float idleLineY() { return hz - 55f; }

    private static final float GAME_MINUTES_PER_SECOND = 3f;
    private static final float WEATHER_CHANGE_INTERVAL = 120f;
    private static final float CAST_DURATION           = 0.5f;
    private static final float REEL_DURATION           = 0.4f;
    private static final float BITING_WINDOW           = 2.5f;

    private static final Color[] WATER_PAL = {
        new Color(0.16f, 0.46f, 0.72f, 1f),
        new Color(0.03f, 0.05f, 0.16f, 1f),
    };

    private static final Map<Rarity, Color> RARITY_COL = Map.of(
        Rarity.COMMON,    new Color(0.75f, 0.75f, 0.75f, 1f),
        Rarity.UNCOMMON,  new Color(0.30f, 0.85f, 0.30f, 1f),
        Rarity.RARE,      new Color(0.30f, 0.55f, 1.00f, 1f),
        Rarity.EPIC,      new Color(0.75f, 0.30f, 0.90f, 1f),
        Rarity.LEGENDARY, new Color(1.00f, 0.82f, 0.10f, 1f)
    );

    private static final Color BITE_COLOR = new Color(1.00f, 0.90f, 0.10f, 1f);
    private static final Color COIN_GOLD  = new Color(1.00f, 0.82f, 0.15f, 1f);
    private static final Color COIN_DARK  = new Color(0.75f, 0.58f, 0.05f, 1f);

    private static final Color PLANK      = new Color(0.42f, 0.28f, 0.15f, 1f);
    private static final Color PLANK_TOP  = new Color(0.55f, 0.40f, 0.22f, 1f);
    private static final Color PLANK_LINE = new Color(0.33f, 0.20f, 0.09f, 1f);
    private static final Color PLANK_SIDE = new Color(0.37f, 0.23f, 0.11f, 1f);
    private static final Color POST       = new Color(0.32f, 0.20f, 0.10f, 1f);
    private static final Color LINE_COL = new Color(0.80f, 0.80f, 0.80f, 1f);


    private final FishingVillageGame game;

    private OrthographicCamera camera;
    private ShapeRenderer      shapes;
    private SpriteBatch        batch;
    private BitmapFont         font;

    private GameClock     clock;
    private WeatherSystem weatherSystem;
    private Random        rng;

    private float clockAccum   = 0f;
    private float weatherTimer = 0f;

    private Sound[] waterSounds;
    private float   ambienceTimer    = 0f;
    private float   nextAmbienceTime = 0f;

    private Sound castSound;
    private Sound lureBobSound;
    private Sound fishBiteSound;
    private Sound bubblesSound;
    private Sound waveSound;
    private Sound junkSound;
    private Sound shinyCaughtSound;

    private float waveTime           = 0f;
    private float waveEventTimer     = 0f;
    private float nextWaveEvent      = 0f;
    private float waveEventIntensity = 0f;

    private static final int   SHIMMER_COUNT = 35;
    private final float[] shimmerX    = new float[SHIMMER_COUNT];
    private final float[] shimmerY    = new float[SHIMMER_COUNT];
    private final float[] shimmerAge  = new float[SHIMMER_COUNT];
    private final float[] shimmerLife = new float[SHIMMER_COUNT];
    private float shimmerTime = 0f;

    private static final int   SPLASH_COUNT    = 12;
    private static final float SPLASH_DURATION = 0.65f;
    private static final float SPLASH_GRAVITY  = -300f;
    private final float[] splashX  = new float[SPLASH_COUNT];
    private final float[] splashY  = new float[SPLASH_COUNT];
    private final float[] splashVX = new float[SPLASH_COUNT];
    private final float[] splashVY = new float[SPLASH_COUNT];
    private float splashTimer   = -1f;
    private float splashCentreX, splashCentreY;

    private float fogTime = 0f;
    private static final int RAIN_COUNT = 200;
    private final float[] rainX = new float[RAIN_COUNT];
    private final float[] rainY = new float[RAIN_COUNT];

    private final Color currentWater = new Color();

    private FishingState fishingState = FishingState.IDLE;
    private float        stateTimer  = 0f;
    private float        waitTime    = 0f;

    private float bobberX = FISHER_X + 315f;
    private float bobberY = hz - 55f;
    private float targetX, targetY;

    private Fish     caughtFish   = null;
    private JunkType caughtJunk   = null;
    private boolean  caughtShiny  = false;
    private double   caughtWeight = 0;
    private int      caughtValue  = 0;
    private boolean  lastWasMiss  = false;
    private String   missMessage  = "";
    private boolean bagOpen        = false;
    private int     bagScrollOffset = 0;
    private static final int BAG_VISIBLE = 10;

    private boolean journalOpen        = false;
    private int     journalTab         = 0;
    private int     journalScrollOffset = 0;
    private static final int JOURNAL_VISIBLE = 12;

    private Texture[] bgDayClear;
    private Texture[] bgDayOvercast;
    private Texture[] bgDayRain;
    private Texture[] bgNight;
    private Texture[] bgNightOvercast;

    private Texture   texMerchant;
    private Texture   texFisherStanding;
    private Texture   texFisherSitting;
    private Texture   texRodBasic, texRodAdvanced, texRodMaster;
    private Texture[] texBobber;
    private Map<String, Texture> fishTextures;
    private Map<String, Texture> shinyFishTextures;
    private Map<JunkType, Texture> junkTextures;
    private float bobberAnimTimer = 0f;

    private boolean devWindowOpen    = false;
    private int     devSection       = 0;
    private int     devTimeCursor    = 0;
    private int     devWeatherCursor = 0;
    private int     devMoneyCursor   = 0;
    private static final int[] DEV_MONEY_AMOUNTS = { 100, 500, 1000, 5000, 10000 };
    // Cheat code: type D-E-V within 3 seconds to open dev window
    private static final int[]  DEV_SEQ     = { Input.Keys.D, Input.Keys.E, Input.Keys.V };
    private static final float  DEV_SEQ_TTL = 3f;
    private int   devSeqStep  = 0;
    private float devSeqTimer = 0f;

    private float mgZonePos       = 0.5f;
    private float mgZoneDir       = 1f;
    private float mgZoneSpeed     = 0f;
    private float mgZoneHalfWidth = 0.12f;
    private float mgBarWidth      = 500f;
    private float mgTimeLimit     = 30f;
    private float mgTimeElapsed   = 0f;
    private int   mgAttempts      = 0; // -1 = unlimited
    private float mgMissFlash     = 0f;

    private float   lightningTimer = 4f;
    private float   lightningFlash = 0f;
    private float[] boltX          = new float[10];
    private float[] boltY          = new float[10];
    private int     boltPoints     = 0;
    private static final float LIGHTNING_FADE = 0.25f;

    private Texture[] bgCurLayers   = null;
    private Texture[] bgFromLayers  = null;
    private float     skyTransition = 1f;
    private static final float SKY_FADE_DURATION = 5f;

    public FishingScreen(FishingVillageGame game) {
        this.game = game;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, W, H);
        shapes = new ShapeRenderer();
        batch  = new SpriteBatch();
        font   = Fonts.ui;

        int savedMinute = game.gameState.getClockMinute();
        clock         = new GameClock(savedMinute / 60, savedMinute % 60);
        weatherSystem = new WeatherSystem(Weather.CLEAR);
        rng           = new Random();

        waterSounds = new Sound[]{
            Gdx.audio.newSound(Gdx.files.internal("water_ambience_part01.ogg")),
            Gdx.audio.newSound(Gdx.files.internal("water_ambience_part02.ogg")),
            Gdx.audio.newSound(Gdx.files.internal("water_ambience_part03.ogg")),
            Gdx.audio.newSound(Gdx.files.internal("water_ambience_part04.ogg")),
        };
        nextAmbienceTime = 10f + rng.nextFloat() * 15f;

        castSound     = Gdx.audio.newSound(Gdx.files.internal("cast.wav"));
        lureBobSound  = Gdx.audio.newSound(Gdx.files.internal("lure_bob.wav"));
        fishBiteSound = Gdx.audio.newSound(Gdx.files.internal("fishbite.wav"));
        bubblesSound  = Gdx.audio.newSound(Gdx.files.internal("bubbles.wav"));
        waveSound     = Gdx.audio.newSound(Gdx.files.internal("wave.wav"));
        junkSound        = Gdx.audio.newSound(Gdx.files.internal("fart.wav"));
        shinyCaughtSound = Gdx.audio.newSound(Gdx.files.internal("shinycaught.wav"));
        nextWaveEvent = 20f + rng.nextFloat() * 15f;
        for (int i = 0; i < RAIN_COUNT; i++) {
            rainX[i] = rng.nextFloat() * W;
            rainY[i] = rng.nextFloat() * H;
        }
        for (int i = 0; i < SHIMMER_COUNT; i++) {
            shimmerX[i]    = 730f + rng.nextFloat() * 530f;
            shimmerY[i]    = 20f  + rng.nextFloat() * (hz - 40f);
            shimmerLife[i] = 0.4f + rng.nextFloat() * 1.3f;
            shimmerAge[i]  = rng.nextFloat() * shimmerLife[i]; // stagger so they don't all flash at once
        }

        bgDayClear = new Texture[] {
            tex("background/DayTime_Clear_Rainy/bluesky_and_ocean.png"),
            tex("background/DayTime_Clear_Rainy/cloud_shader.png"),
            tex("background/DayTime_Clear_Rainy/clouds_white.png"),
        };
        bgDayOvercast = new Texture[] {
            tex("background/DayTime_Overcast/bluesky_and_ocean.png"),
            tex("background/DayTime_Overcast/overcast_clouds_back.png"),
            tex("background/DayTime_Overcast/overcast_clouds_left_right.png"),
            tex("background/DayTime_Overcast/overcast_clouds_full.png"),
        };
        bgDayRain = new Texture[] {
            tex("background/DayTime_Clear_Rainy/bluesky_and_ocean.png"),
            tex("background/DayTime_Clear_Rainy/cloud_shader.png"),
            tex("background/DayTime_Clear_Rainy/clouds_gray.png"),
        };
        bgNight = new Texture[] {
            tex("background/NightTime_1/clear_night_sky.png"),
            tex("background/NightTime_1/moon_reflection_on_water.png"),
            tex("background/NightTime_1/fullmoon.png"),
            tex("background/NightTime_1/clouds.png"),
        };
        bgNightOvercast = new Texture[] {
            tex("background/NightTime_1/clear_night_sky.png"),
            tex("background/NightTime_1/moon_reflection_on_water.png"),
            tex("background/NightTime_1/fullmoon.png"),
            tex("background/NightTime_1/clouds.png"),
            tex("background/NightTime_1/overcast_night_clouds.png"),
        };

        texMerchant       = pixTex("sprites/merchant-npc.png");
        texFisherStanding = pixTex("sprites/Fisherman/fisherman_standing.png");
        texFisherSitting  = pixTex("sprites/Fisherman/fisherman_sitting.png");
        texRodBasic    = pixTex("sprites/Rods/basicrod1.png");
        texRodAdvanced = pixTex("sprites/Rods/advrod1.png");
        texRodMaster   = pixTex("sprites/Rods/mastrod21.png");
        texBobber = new Texture[]{
            pixTex("sprites/Bobber/bober1.png"),
            pixTex("sprites/Bobber/bober2.png"),
            pixTex("sprites/Bobber/bober3.png"),
        };
        fishTextures = new HashMap<>();
        fishTextures.put("sardine",            pixTex("sprites/Fish/Common/sardines.png"));
        fishTextures.put("herring",            pixTex("sprites/Fish/Common/herring.png"));
        fishTextures.put("carp",               pixTex("sprites/Fish/Common/carp.png"));
        fishTextures.put("catfish",            pixTex("sprites/Fish/Common/catfish.png"));
        fishTextures.put("perch",              pixTex("sprites/Fish/Common/perch.png"));
        fishTextures.put("bass",               pixTex("sprites/Fish/Uncommon/Bass.png"));
        fishTextures.put("trout",              pixTex("sprites/Fish/Uncommon/Trout.png"));
        fishTextures.put("pike",               pixTex("sprites/Fish/Uncommon/pike.png"));
        fishTextures.put("eel",                pixTex("sprites/Fish/Uncommon/eel.png"));
        fishTextures.put("bream",              pixTex("sprites/Fish/Uncommon/Bream.png"));
        fishTextures.put("salmon",             pixTex("sprites/Fish/Rare/Salmon.png"));
        fishTextures.put("tuna",               pixTex("sprites/Fish/Rare/tuna.png"));
        fishTextures.put("swordfish",          pixTex("sprites/Fish/Rare/swordfish.png"));
        fishTextures.put("sturgeon",           pixTex("sprites/Fish/Rare/sturgeon.png"));
        fishTextures.put("shark",              pixTex("sprites/Fish/Epic/chork.png"));
        fishTextures.put("manta_ray",          pixTex("sprites/Fish/Epic/mantaray.png"));
        fishTextures.put("oarfish",            pixTex("sprites/Fish/Epic/oarfesh.png"));
        fishTextures.put("dragon_koi",         pixTex("sprites/Fish/Legendary/koi.png"));
        fishTextures.put("ancient_coelacanth", pixTex("sprites/Fish/Legendary/coe.png"));
        fishTextures.put("leviathan_eel",      pixTex("sprites/Fish/Legendary/levieel.png"));

        shinyFishTextures = new HashMap<>();
        shinyFishTextures.put("sardine",            pixTex("sprites/Fish Variants/Common/Ssardines_shiny_sardine.png"));
        shinyFishTextures.put("herring",            pixTex("sprites/Fish Variants/Common/Sherring_shiny_herring.png"));
        shinyFishTextures.put("carp",               pixTex("sprites/Fish Variants/Common/Scarp_shiny_carp.png"));
        shinyFishTextures.put("catfish",            pixTex("sprites/Fish Variants/Common/Scatfish_shiny_catfish.png"));
        shinyFishTextures.put("perch",              pixTex("sprites/Fish Variants/Common/Sperch_shiny_perch.png"));
        shinyFishTextures.put("bass",               pixTex("sprites/Fish Variants/Uncommon/SBass_shiny_bass.png"));
        shinyFishTextures.put("trout",              pixTex("sprites/Fish Variants/Uncommon/STrout_shiny_trout.png"));
        shinyFishTextures.put("pike",               pixTex("sprites/Fish Variants/Uncommon/Spike_shiny_pike.png"));
        shinyFishTextures.put("eel",                pixTex("sprites/Fish Variants/Uncommon/Seel_shiny_eel.png"));
        shinyFishTextures.put("bream",              pixTex("sprites/Fish Variants/Uncommon/SBream_shiny_bream.png"));
        shinyFishTextures.put("salmon",             pixTex("sprites/Fish Variants/Rare/SSalmon_shiny_salmon.png"));
        shinyFishTextures.put("tuna",               pixTex("sprites/Fish Variants/Rare/Stuna_shiny_tuna.png"));
        shinyFishTextures.put("swordfish",          pixTex("sprites/Fish Variants/Rare/Sswordfish_shiny_swordfish.png"));
        shinyFishTextures.put("sturgeon",           pixTex("sprites/Fish Variants/Rare/Ssturgeon_shiny_sturgeon.png"));
        shinyFishTextures.put("shark",              pixTex("sprites/Fish Variants/Epic/Schork_shiny_chork.png"));
        shinyFishTextures.put("manta_ray",          pixTex("sprites/Fish Variants/Epic/Smantaray_shiny_mantaray.png"));
        shinyFishTextures.put("oarfish",            pixTex("sprites/Fish Variants/Epic/Soarfesh_shiny_oarfish.png"));
        shinyFishTextures.put("dragon_koi",         pixTex("sprites/Fish Variants/Legendary/Skoi_shiny_koi.png"));
        shinyFishTextures.put("ancient_coelacanth", pixTex("sprites/Fish Variants/Legendary/Scoe_shiny_coe.png"));
        shinyFishTextures.put("leviathan_eel",      pixTex("sprites/Fish Variants/Legendary/Slevieel_shiny_levi.png"));

        junkTextures = new EnumMap<>(JunkType.class);
        junkTextures.put(JunkType.BOOT, pixTex("sprites/boots.png"));
        junkTextures.put(JunkType.KELP, pixTex("sprites/kelp.png"));
    }

    private static Texture tex(String path) {
        return new Texture(Gdx.files.internal(path));
    }

    private static Texture pixTex(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return t;
    }

    @Override
    public void render(float delta) {
        clockAccum += delta * GAME_MINUTES_PER_SECOND;
        int mins = (int) clockAccum;
        if (mins > 0) { clock.advance(mins); clockAccum -= mins; }

        weatherTimer += delta;
        if (weatherTimer >= WEATHER_CHANGE_INTERVAL) {
            weatherSystem.transition(rng);
            weatherTimer = 0f;
        }

        bobberAnimTimer += delta;
        updateFishing(delta);
        updateAmbience(delta);
        updateWaves(delta);
        updateShimmer(delta);
        updateSplash(delta);

        TimeOfDay time    = clock.getTimeOfDay();
        Weather   weather = weatherSystem.getCurrent();
        updateWeather(weather, delta);
        if (weather == Weather.STORM) {
            lightningTimer -= delta;
            if (lightningTimer <= 0f) {
                triggerLightning();
                lightningTimer = 3f + rng.nextFloat() * 6f;
            }
        }
        if (lightningFlash > 0f) lightningFlash = Math.max(0f, lightningFlash - delta / LIGHTNING_FADE);
        lerpPalette(WATER_PAL, time, currentWater);


        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();

        drawBackground(time, weather, delta);

        shapes.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        drawWaves();
        drawWaterShimmer();
        drawSplash();
        drawDock();

        shapes.end();

        // Merchant drawn after dock but before stall so stall overlaps him
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        float mw = texMerchant.getWidth()  * 1.8f;
        float mh = texMerchant.getHeight() * 1.8f;
        batch.draw(texMerchant, 155f - mw * 0.5f, hz, mw, mh);
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        drawWharfStall(time == TimeOfDay.NIGHT);
        drawFishingLine();
        drawWeatherEffects(weather);

        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        drawSprites();
        if (lightningFlash > 0f) drawLightningFlash();
        drawHUD(time, weather);

        if (bagOpen) drawBagPanel();

        if (journalOpen) drawJournalPanel();

        if (devWindowOpen) drawDevWindow();

        if (fishingState == FishingState.MINIGAME) {
            drawMinigame();
        }

        if (fishingState == FishingState.SHOW_RESULT) {
            drawResultPanel();
        }

    }

    private void updateFishing(float delta) {
        boolean space = Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
        boolean tab   = Gdx.input.isKeyJustPressed(Input.Keys.TAB);

        if (fishingState != FishingState.MINIGAME && fishingState != FishingState.SHOW_RESULT) {
            if (devSeqStep > 0) devSeqTimer -= delta;
            if (devSeqTimer <= 0f) devSeqStep = 0;

            if (Gdx.input.isKeyJustPressed(DEV_SEQ[devSeqStep])) {
                devSeqStep++;
                devSeqTimer = DEV_SEQ_TTL;
                if (devSeqStep == DEV_SEQ.length) {
                    devSeqStep = 0;
                    devWindowOpen = !devWindowOpen;
                    if (devWindowOpen) {
                        TimeOfDay cur = clock.getTimeOfDay();
                        TimeOfDay[] periods = TimeOfDay.values();
                        for (int i = 0; i < periods.length; i++) if (periods[i] == cur) { devTimeCursor = i; break; }
                        Weather curW = weatherSystem.getCurrent();
                        Weather[] weathers = Weather.values();
                        for (int i = 0; i < weathers.length; i++) if (weathers[i] == curW) { devWeatherCursor = i; break; }
                    }
                }
            } else if (devSeqStep > 0 && anyKeyJustPressed()) {
                devSeqStep = 0;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            game.toggleMute();
        }

        if (devWindowOpen) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) { devWindowOpen = false; return; }
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT))  devSection = Math.max(devSection - 1, 0);
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) devSection = Math.min(devSection + 1, 2);
            if (devSection == 0) {
                TimeOfDay[] periods = TimeOfDay.values();
                if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                    devTimeCursor = (devTimeCursor - 1 + periods.length) % periods.length;
                    clock.setMinuteOfDay(periods[devTimeCursor].startHour * 60);
                    clockAccum = 0f;
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                    devTimeCursor = (devTimeCursor + 1) % periods.length;
                    clock.setMinuteOfDay(periods[devTimeCursor].startHour * 60);
                    clockAccum = 0f;
                }
            } else if (devSection == 1) {
                Weather[] weathers = Weather.values();
                if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                    devWeatherCursor = (devWeatherCursor - 1 + weathers.length) % weathers.length;
                    weatherSystem.setCurrent(weathers[devWeatherCursor]);
                    weatherTimer = 0f;
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                    devWeatherCursor = (devWeatherCursor + 1) % weathers.length;
                    weatherSystem.setCurrent(weathers[devWeatherCursor]);
                    weatherTimer = 0f;
                }
            } else {
                if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                    devMoneyCursor = (devMoneyCursor - 1 + DEV_MONEY_AMOUNTS.length) % DEV_MONEY_AMOUNTS.length;
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                    devMoneyCursor = (devMoneyCursor + 1) % DEV_MONEY_AMOUNTS.length;
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                    game.gameState.addCoins(DEV_MONEY_AMOUNTS[devMoneyCursor]);
                }
            }
            return;
        }

        switch (fishingState) {
            case IDLE:
                if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
                    bagOpen = !bagOpen;
                    bagScrollOffset = 0;
                    if (bagOpen) journalOpen = false;
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
                    journalOpen = !journalOpen;
                    journalScrollOffset = 0;
                    journalTab = 0;
                    if (journalOpen) bagOpen = false;
                }
                if (bagOpen) {
                    int maxSlots = game.gameState.getFishBag().getMaxSlots();
                    int maxScroll = Math.max(0, maxSlots - BAG_VISIBLE);
                    if (Gdx.input.isKeyJustPressed(Input.Keys.UP))
                        bagScrollOffset = Math.max(0, bagScrollOffset - 1);
                    if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN))
                        bagScrollOffset = Math.min(maxScroll, bagScrollOffset + 1);
                }
                if (journalOpen) {
                    if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT))  { journalTab = 0; journalScrollOffset = 0; }
                    if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) { journalTab = 1; journalScrollOffset = 0; }
                    if (journalTab == 0) {
                        int maxScroll = Math.max(0, FishRegistry.getAll().size() - JOURNAL_VISIBLE);
                        if (Gdx.input.isKeyJustPressed(Input.Keys.UP))
                            journalScrollOffset = Math.max(0, journalScrollOffset - 1);
                        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN))
                            journalScrollOffset = Math.min(maxScroll, journalScrollOffset + 1);
                    }
                }
                if (space) {
                    if (!game.gameState.getFishBag().isFull()) {
                        bagOpen = false;
                        journalOpen = false;
                        startCast();
                    }
                }
                if (tab) game.setScreen(game.shopScreen);
                break;

            case CASTING:
                stateTimer += delta;
                float castT = MathUtils.clamp(stateTimer / CAST_DURATION, 0f, 1f);
                bobberX = MathUtils.lerp(rodTipX(), targetX, castT);
                bobberY = MathUtils.lerp(rodTipY(), targetY, castT);
                if (stateTimer >= CAST_DURATION) {
                    bobberX = targetX; bobberY = targetY;
                    waitTime   = 4f + rng.nextFloat() * 14f;
                    stateTimer = 0f;
                    fishingState = FishingState.WAITING;
                    triggerSplash(bobberX, bobberY);
                    lureBobSound.play(0.30f);
                }
                break;

            case WAITING:
                stateTimer += delta;
                if (stateTimer >= waitTime) {
                    stateTimer   = 0f;
                    fishingState = FishingState.BITING;
                    fishBiteSound.play(0.45f);
                }
                break;

            case BITING:
                stateTimer += delta;
                if (space) {
                    CatchResult result = FishRegistry.rollCatch(
                        rng, clock.getTimeOfDay(), weatherSystem.getCurrent(),
                        game.gameState.getRod(), game.gameState.getEquippedBait());
                    if (result instanceof CatchResult.JunkCatch jc) {
                        caughtFish  = null;
                        caughtJunk  = jc.junk();
                        caughtValue = jc.junk().value;
                        game.gameState.getFishBag().add(new CaughtFish(null, jc.junk(), 0, false, jc.junk().value));
                        game.gameState.recordCatch();
                        lastWasMiss  = false;
                        junkSound.play(0.6f);
                        bubblesSound.play(0.35f);
                        stateTimer   = 0f;
                        fishingState = FishingState.REELING;
                    } else {
                        caughtFish   = ((CatchResult.FishCatch) result).fish();
                        caughtJunk   = null;
                        caughtShiny  = caughtFish.rollShiny(rng);
                        caughtWeight = caughtFish.rollWeight(rng);
                        caughtValue  = caughtFish.sellValue(caughtWeight, caughtShiny);
                        startMinigame(caughtFish.rarity());
                    }
                } else if (stateTimer >= BITING_WINDOW) {
                    lastWasMiss  = true;
                    missMessage  = "It got away!";
                    stateTimer   = 0f;
                    fishingState = FishingState.SHOW_RESULT;
                }
                break;

            case MINIGAME:
                mgZonePos += mgZoneDir * mgZoneSpeed * delta;
                if (mgZonePos >= 1f - mgZoneHalfWidth) { mgZonePos = 1f - mgZoneHalfWidth; mgZoneDir = -1f; }
                if (mgZonePos <= mgZoneHalfWidth)       { mgZonePos = mgZoneHalfWidth;       mgZoneDir =  1f; }
                if (mgMissFlash > 0f) mgMissFlash -= delta;
                mgTimeElapsed += delta;
                if (mgTimeElapsed >= mgTimeLimit) {
                    lastWasMiss  = true;
                    missMessage  = "The fish got away!";
                    caughtFish   = null;
                    stateTimer   = 0f;
                    fishingState = FishingState.SHOW_RESULT;
                    break;
                }
                if (space) {
                    if (Math.abs(0.5f - mgZonePos) <= mgZoneHalfWidth) {
                        commitCatch();
                        bubblesSound.play(0.35f);
                        stateTimer   = 0f;
                        fishingState = FishingState.REELING;
                    } else {
                        mgMissFlash = 0.5f;
                        if (mgAttempts > 0) {
                            mgAttempts--;
                            if (mgAttempts == 0) {
                                lastWasMiss  = true;
                                missMessage  = "The fish slipped the hook!";
                                caughtFish   = null;
                                stateTimer   = 0f;
                                fishingState = FishingState.SHOW_RESULT;
                            }
                        }
                    }
                }
                break;

            case REELING:
                stateTimer += delta;
                float reelT = MathUtils.clamp(stateTimer / REEL_DURATION, 0f, 1f);
                bobberX = MathUtils.lerp(targetX, rodTipX(), reelT);
                bobberY = MathUtils.lerp(targetY, rodTipY(), reelT);
                if (stateTimer >= REEL_DURATION) {
                    stateTimer   = 0f;
                    fishingState = FishingState.SHOW_RESULT;
                }
                break;

            case SHOW_RESULT:
                if (space) {
                    caughtFish   = null;
                    caughtJunk   = null;
                    bobberX      = rodTipX();
                    bobberY      = idleLineY();
                    stateTimer   = 0f;
                    fishingState = FishingState.IDLE;
                }
                break;
        }
    }

    private void startCast() {
        game.gameState.consumeBait();
        targetX      = 750f + rng.nextFloat() * 430f;  // 750-1180, right of dock (ends ~x710)
        targetY      =  20f + rng.nextFloat() * 220f;  // 20-240, full visible water area
        stateTimer   = 0f;
        fishingState = FishingState.CASTING; // must be set before rodTipX/Y so armY() uses sitting values
        bobberX      = rodTipX();
        bobberY      = rodTipY();
        castSound.play(0.40f);
    }

    private void commitCatch() {
        game.gameState.getFishBag().add(new CaughtFish(caughtFish, caughtWeight, caughtShiny, caughtValue));
        game.gameState.recordCatch();
        game.gameState.recordFishCatch(caughtFish.id(), caughtWeight, caughtShiny);
        checkAchievements();
        if (caughtShiny) shinyCaughtSound.play(1.0f);
        lastWasMiss = false;
    }

    private void checkAchievements() {
        var gs = game.gameState;
        int total = gs.getTotalCatches();
        if (total >= 1)   gs.unlockAchievement(Achievement.FIRST_CATCH);
        if (total >= 10)  gs.unlockAchievement(Achievement.CATCH_10);
        if (total >= 50)  gs.unlockAchievement(Achievement.CATCH_50);
        if (total >= 100) gs.unlockAchievement(Achievement.CATCH_100);

        FishRecord rec = gs.getFishRecord(caughtFish.id());
        if (rec.shinyUnlocked) gs.unlockAchievement(Achievement.FIRST_SHINY);

        Rarity r = caughtFish.rarity();
        if (r.ordinal() >= Rarity.RARE.ordinal())      gs.unlockAchievement(Achievement.FIRST_RARE);
        if (r.ordinal() >= Rarity.EPIC.ordinal())      gs.unlockAchievement(Achievement.FIRST_EPIC);
        if (r == Rarity.LEGENDARY)                     gs.unlockAchievement(Achievement.FIRST_LEGENDARY);

        if (FishRegistry.getByRarity(Rarity.COMMON).stream()
                .allMatch(f -> gs.getFishRecord(f.id()).discovered))
            gs.unlockAchievement(Achievement.ALL_COMMON);
        if (FishRegistry.getByRarity(Rarity.UNCOMMON).stream()
                .allMatch(f -> gs.getFishRecord(f.id()).discovered))
            gs.unlockAchievement(Achievement.ALL_UNCOMMON);
        if (FishRegistry.getByRarity(Rarity.RARE).stream()
                .allMatch(f -> gs.getFishRecord(f.id()).discovered))
            gs.unlockAchievement(Achievement.ALL_RARE);
        if (FishRegistry.getAll().stream()
                .allMatch(f -> gs.getFishRecord(f.id()).discovered))
            gs.unlockAchievement(Achievement.ALL_FISH);
    }

    private void startMinigame(Rarity rarity) {
        mgTimeElapsed = 0f;
        mgTimeLimit   = 30f;
        mgMissFlash   = 0f;
        mgZoneDir     = rng.nextBoolean() ? 1f : -1f;
        switch (rarity) {
            case COMMON    -> { mgBarWidth = 340f; mgZoneSpeed = 0.13f; mgZoneHalfWidth = 0.16f; mgAttempts = -1; }
            case UNCOMMON  -> { mgBarWidth = 340f; mgZoneSpeed = 0.22f; mgZoneHalfWidth = 0.12f; mgAttempts =  4; }
            case RARE      -> { mgBarWidth = 460f; mgZoneSpeed = 0.33f; mgZoneHalfWidth = 0.08f; mgAttempts =  3; }
            case EPIC      -> { mgBarWidth = 460f; mgZoneSpeed = 0.50f; mgZoneHalfWidth = 0.05f; mgAttempts =  2; }
            case LEGENDARY -> { mgBarWidth = 460f; mgZoneSpeed = 0.70f; mgZoneHalfWidth = 0.03f; mgAttempts =  1; }
        }
        if (weatherSystem.getCurrent() == Weather.STORM && rarity.ordinal() >= Rarity.RARE.ordinal()) {
            mgZoneSpeed *= 1.35f;
        }
        float lo   = mgZoneHalfWidth;
        float hi   = 1f - mgZoneHalfWidth;
        float dead = 0.10f;
        mgZonePos = rng.nextBoolean()
            ? lo  + rng.nextFloat() * Math.max(0f, 0.5f - dead - lo)
            : (0.5f + dead) + rng.nextFloat() * Math.max(0f, hi - 0.5f - dead);
        fishingState = FishingState.MINIGAME;
    }

    private static final float CLOUD_OFFSET = 60f;

    private void drawBackground(TimeOfDay time, Weather weather, float delta) {
        boolean isNight = time == TimeOfDay.NIGHT;
        Texture[] newLayers;
        if (isNight) {
            newLayers = (weather == Weather.OVERCAST || weather == Weather.RAIN || weather == Weather.STORM)
                ? bgNightOvercast : bgNight;
        } else {
            newLayers = switch (weather) {
                case OVERCAST        -> bgDayOvercast;
                case RAIN, STORM     -> bgDayRain;
                default              -> bgDayClear;
            };
        }

        if (bgCurLayers == null) {
            bgCurLayers   = newLayers;
            skyTransition = 1f;
        } else if (newLayers != bgCurLayers) {
            bgFromLayers  = bgCurLayers;
            bgCurLayers   = newLayers;
            skyTransition = 0f;
        }

        if (skyTransition < 1f) skyTransition = Math.min(1f, skyTransition + delta / SKY_FADE_DURATION);

        float overlayY = hz - CLOUD_OFFSET;
        float overlayH = H - hz + CLOUD_OFFSET;
        float skyFrac  = overlayH / H;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (skyTransition < 1f && bgFromLayers != null) {
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(bgFromLayers[0], 0, 0, W, H);
            for (int i = 1; i < bgFromLayers.length; i++) {
                Texture t = bgFromLayers[i];
                batch.draw(t, 0f, overlayY, W, overlayH,
                    0, 0, t.getWidth(), (int)(t.getHeight() * skyFrac), false, false);
            }
        }

        batch.setColor(1f, 1f, 1f, skyTransition);
        batch.draw(bgCurLayers[0], 0, 0, W, H);
        for (int i = 1; i < bgCurLayers.length; i++) {
            Texture t = bgCurLayers[i];
            batch.draw(t, 0f, overlayY, W, overlayH,
                0, 0, t.getWidth(), (int)(t.getHeight() * skyFrac), false, false);
        }

        batch.setColor(Color.WHITE);
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawWharfStall(boolean isNight) {
        Color cTop    = isNight ? new Color(0.35f, 0.22f, 0.10f, 1f) : PLANK_TOP;
        Color cBody   = isNight ? new Color(0.22f, 0.14f, 0.06f, 1f) : PLANK;
        Color cSide   = isNight ? new Color(0.18f, 0.10f, 0.04f, 1f) : PLANK_SIDE;
        Color cPost   = isNight ? new Color(0.15f, 0.09f, 0.03f, 1f) : POST;
        Color cAwn1   = isNight ? new Color(0.40f, 0.08f, 0.06f, 1f) : new Color(0.80f, 0.18f, 0.12f, 1f);
        Color cAwn2   = isNight ? new Color(0.35f, 0.32f, 0.22f, 1f) : new Color(0.95f, 0.92f, 0.80f, 1f);
        Color cDrop   = isNight ? new Color(0.30f, 0.06f, 0.04f, 1f) : new Color(0.60f, 0.12f, 0.08f, 1f);
        Color cBarrel = isNight ? new Color(0.18f, 0.12f, 0.06f, 1f) : new Color(0.45f, 0.30f, 0.12f, 1f);
        Color cHoop   = isNight ? new Color(0.12f, 0.08f, 0.04f, 1f) : new Color(0.28f, 0.18f, 0.08f, 1f);

        float sx = 35f;
        float sy = hz;
        float sw = 240f;
        float ch = 55f;
        float ct = 10f;

        shapes.setColor(cBody);
        shapes.rect(sx, sy, sw, ch);
        shapes.setColor(cTop);
        shapes.rect(sx, sy + ch, sw, ct);
        shapes.setColor(cSide);
        shapes.rect(sx + sw, sy, 10f, ch + ct);

        float postBase = sy + ch + ct;
        shapes.setColor(cPost);
        shapes.rect(sx + 8f,        postBase, 8f, 140f);
        shapes.rect(sx + sw - 16f,  postBase, 8f, 140f);

        float cx = sx - 28f;
        float cw = sw + 56f;
        float cy = postBase + 140f;
        float stripeW = cw / 6f;
        for (int i = 0; i < 6; i++) {
            shapes.setColor((i % 2 == 0) ? cAwn1 : cAwn2);
            shapes.rect(cx + i * stripeW, cy, stripeW, 28f);
        }
        shapes.setColor(cDrop);
        shapes.rect(cx, cy - 16f, cw, 16f);

        shapes.setColor(cBarrel);
        shapes.rect(sx + 24f, postBase, 44f, 52f);
        shapes.setColor(cHoop);
        shapes.rect(sx + 24f, postBase +  6f, 44f, 4f);
        shapes.rect(sx + 24f, postBase + 22f, 44f, 4f);
        shapes.rect(sx + 24f, postBase + 38f, 44f, 4f);

        shapes.setColor(cBody);
        shapes.rect(sx + sw - 72f, postBase, 56f, 40f);
        shapes.setColor(cSide);
        shapes.rect(sx + sw - 72f, postBase + 24f, 56f, 4f);
        shapes.rect(sx + sw - 46f, postBase, 4f, 40f);
    }

    private void drawDock() {
        int[] postX = {100, 270, 460, 640};

        shapes.setColor(POST);
        for (int px : postX) {
            shapes.rect(px - 8f, hz - 240f, 16f, 240f);
        }

        shapes.rect(postX[0] - 4f, hz - 112f, (postX[postX.length - 1] - postX[0]) + 8f, 10f);

        shapes.setColor(PLANK_SIDE);
        for (int i = 0; i < postX.length - 1; i++) {
            shapes.rectLine(postX[i],     hz - 240f, postX[i + 1], hz - 112f, 4f);
            shapes.rectLine(postX[i + 1], hz - 240f, postX[i],     hz - 112f, 4f);
        }

        shapes.setColor(PLANK);
        shapes.rect(0, hz - 22f, 710f, 22f);

        shapes.setColor(PLANK_LINE);
        for (float lx = 35f; lx < 710f; lx += 35f) {
            shapes.rect(lx - 1f, hz - 22f, 2f, 17f);
        }

        shapes.setColor(PLANK_TOP);
        shapes.rect(0, hz - 6f, 710f, 6f);

        shapes.setColor(PLANK_SIDE);
        shapes.rect(706f, hz - 26f, 6f, 26f);
    }

    private void drawSprites() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        boolean isStanding = fishingState == FishingState.IDLE || fishingState == FishingState.SHOW_RESULT;

        Texture rod = rodTexture();
        float rw = rod.getWidth()  * ROD_SCALE;
        float rh = rod.getHeight() * ROD_SCALE;
        float rodSX = armX() - 20f * ROD_SCALE;
        float rodSY = armY() - 24f * ROD_SCALE; // 24 = 99 - 75 (rows from bottom to handle)
        batch.draw(rod, rodSX, rodSY, rw, rh);

        Texture fisher = isStanding ? texFisherStanding : texFisherSitting;
        float fw = fisher.getWidth()  * 4f;
        float fh = fisher.getHeight() * 4f;
        float spriteY = isStanding ? hz - 92f : hz - 108f;
        batch.draw(fisher, FISHER_X - fw * 0.1f, spriteY, fw, fh);

        if (fishingState == FishingState.CASTING  || fishingState == FishingState.WAITING
         || fishingState == FishingState.BITING   || fishingState == FishingState.REELING) {
            float visY = bobberY;
            if (fishingState == FishingState.WAITING)
                visY += MathUtils.sin(stateTimer * 3.5f) * 4f;
            else if (fishingState == FishingState.BITING)
                visY -= 6f + MathUtils.sin(stateTimer * 8f) * 3f;
            int frame = (int)(bobberAnimTimer * 6f) % texBobber.length;
            Texture bob = texBobber[frame];
            float bw = bob.getWidth()  * 2f;
            float bh = bob.getHeight() * 2f;
            batch.draw(bob, bobberX - bw / 2f, visY - bh / 2f, bw, bh);
        }

        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }


    private Texture rodTexture() {
        return switch (game.gameState.getRod()) {
            case BASIC    -> texRodBasic;
            case ADVANCED -> texRodAdvanced;
            case MASTER   -> texRodMaster;
        };
    }

    private void drawFishingLine() {
        float visY = bobberY;
        if (fishingState == FishingState.WAITING) {
            visY += MathUtils.sin(stateTimer * 3.5f) * 4f;
        } else if (fishingState == FishingState.BITING) {
            visY -= 6f + MathUtils.sin(stateTimer * 8f) * 3f;
        }

        shapes.setColor(LINE_COL);
        shapes.rectLine(rodTipX(), rodTipY(), bobberX, visY, 1.5f);
    }

    private void drawHUD(TimeOfDay time, Weather weather) {
        String prompt = getPromptText();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        shapes.setColor(0f, 0f, 0f, 0.48f);
        shapes.rect(8f, H - 68f, 468f, 60f);

        shapes.setColor(0f, 0f, 0f, 0.48f);
        shapes.rect(W - 222f, H - 100f, 214f, 92f);

        if (prompt != null) {
            shapes.setColor(0f, 0f, 0f, 0.52f);
            shapes.rect(0f, 0f, W, 50f);
        }

        drawWeatherIcon(weather, 30f, H - 38f);

        shapes.setColor(COIN_GOLD); shapes.circle(W - 209f, H - 26f, 9f);
        shapes.setColor(COIN_DARK); shapes.circle(W - 209f, H - 26f, 5f);

        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        font.setColor(Color.WHITE);
        font.draw(batch,
            clock.getFormattedTime() + "   " + time.displayName + "   |   " + weather.displayName,
            58f, H - 14f);
        font.setColor(0.78f, 0.78f, 0.78f, 1f);
        boolean isNight = time == TimeOfDay.NIGHT;
        font.draw(batch, isNight ? nightFlavor(weather) : weather.flavor, 58f, H - 32f);
        font.setColor(0.52f, 0.52f, 0.52f, 1f);
        font.draw(batch, time.displayName + "  ->  " + nextPeriodName(time), 58f, H - 54f);

        font.setColor(COIN_GOLD);
        font.draw(batch, String.valueOf(game.gameState.getCoins()), W - 195f, H - 20f);
        font.setColor(0.72f, 0.72f, 0.72f, 1f);
        font.draw(batch, "Catches:  " + game.gameState.getTotalCatches(), W - 209f, H - 40f);
        font.setColor(0.55f, 0.55f, 0.60f, 1f);
        font.draw(batch, game.gameState.getRod().displayName, W - 209f, H - 57f);
        BaitType bait = game.gameState.getEquippedBait();
        String baitLabel = bait == BaitType.NONE
            ? "No Bait"
            : bait.displayName + " x" + game.gameState.getBaitCount(bait);
        font.draw(batch, baitLabel, W - 209f, H - 74f);

        if (prompt != null) {
            font.setColor(fishingState == FishingState.BITING ? BITE_COLOR : Color.WHITE);
            font.getData().setScale(1.4f);
            font.draw(batch, prompt, W / 2f - estimateHalfWidth(prompt, 1.4f), 32f);
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
        }

        if (game.musicMuted) {
            font.setColor(0.55f, 0.55f, 0.60f, 1f);
            font.draw(batch, "[F2] Music: OFF", W - 150f, 18f);
            font.setColor(Color.WHITE);
        }

        batch.end();
    }

    private boolean anyKeyJustPressed() {
        for (int k = 0; k < 256; k++) {
            if (Gdx.input.isKeyJustPressed(k)) return true;
        }
        return false;
    }

    private String getPromptText() {
        return switch (fishingState) {
            case IDLE    -> game.gameState.getFishBag().isFull()
                                ? "Bag full!   [TAB] Visit Shop to sell"
                                : "[SPACE] Cast   [B] Bag   [J] Journal   [TAB] Shop";
            case WAITING -> "Waiting for a bite...";
            case BITING  -> "!! BITE !!   [SPACE] Reel in!";
            case MINIGAME -> null;
            default      -> null;
        };
    }

    private static String nightFlavor(Weather weather) {
        return switch (weather) {
            case CLEAR    -> "Moonlight ripples on the water. The deep awakens.";
            case OVERCAST -> "No stars tonight. Something stirs beneath the surface.";
            case RAIN     -> "Rain drums on black water. Everything below is feeding.";
            case STORM    -> "Lightning splits the sky. The deep is churned up and hungry.";
            case FOG      -> "Thick fog swallows the dock. Strange shapes move below.";
        };
    }

    private String nextPeriodName(TimeOfDay time) {
        TimeOfDay[] p = TimeOfDay.values();
        return p[(time.ordinal() + 1) % p.length].displayName;
    }

    private float estimateHalfWidth(String text, float scale) {
        return text.length() * 7.5f * scale / 2f;
    }

    private void drawWeatherIcon(Weather weather, float cx, float cy) {
        switch (weather) {
            case CLEAR -> {
                shapes.setColor(1f, 0.97f, 0.55f, 1f);
                shapes.circle(cx, cy, 8f);
                shapes.setColor(1f, 0.88f, 0.35f, 1f);
                for (int i = 0; i < 8; i++) {
                    float a = i * MathUtils.PI2 / 8f;
                    shapes.rectLine(cx + MathUtils.cos(a) * 11f, cy + MathUtils.sin(a) * 11f,
                                    cx + MathUtils.cos(a) * 16f, cy + MathUtils.sin(a) * 16f, 2f);
                }
            }
            case OVERCAST -> {
                shapes.setColor(0.62f, 0.62f, 0.68f, 1f);
                shapes.circle(cx - 7f, cy - 1f,  8f);
                shapes.circle(cx + 1f, cy + 2f, 10f);
                shapes.circle(cx + 9f, cy - 1f,  7f);
            }
            case RAIN -> {
                shapes.setColor(0.52f, 0.52f, 0.60f, 1f);
                shapes.circle(cx - 5f, cy + 3f, 7f);
                shapes.circle(cx + 3f, cy + 5f, 9f);
                shapes.circle(cx + 10f, cy + 2f, 6f);
                shapes.setColor(0.38f, 0.58f, 0.90f, 1f);
                shapes.rectLine(cx - 5f, cy - 2f, cx - 8f, cy - 11f, 2f);
                shapes.rectLine(cx + 1f, cy - 4f, cx - 1f, cy - 13f, 2f);
                shapes.rectLine(cx + 7f, cy - 2f, cx + 4f, cy - 11f, 2f);
            }
            case STORM -> {
                shapes.setColor(0.30f, 0.30f, 0.38f, 1f);
                shapes.circle(cx - 5f, cy + 4f,  8f);
                shapes.circle(cx + 3f, cy + 6f, 10f);
                shapes.circle(cx + 11f, cy + 3f, 7f);
                shapes.setColor(1f, 0.95f, 0.20f, 1f);
                shapes.rectLine(cx,      cy + 2f, cx - 3f,  cy - 5f,  2.5f);
                shapes.rectLine(cx - 3f, cy - 5f, cx + 2f,  cy - 5f,  2.5f);
                shapes.rectLine(cx + 2f, cy - 5f, cx - 1f,  cy - 14f, 2.5f);
            }
            case FOG -> {
                shapes.setColor(0.70f, 0.73f, 0.76f, 1f);
                float[] widths = { 28f, 20f, 26f, 18f };
                for (int i = 0; i < widths.length; i++) {
                    float ly = cy + 7f - i * 5.5f;
                    shapes.rect(cx - widths[i] / 2f, ly - 1.5f, widths[i], 3f);
                }
            }
        }
    }


    private void drawResultPanel() {
        float pw = 580f;
        float ph = lastWasMiss         ? 160f
                 : caughtJunk != null  ? 280f
                 : caughtShiny         ? 300f : 280f;
        float px = (W - pw) / 2f;
        float py = (H - ph) / 2f;

        Color borderColor = lastWasMiss        ? new Color(0.50f, 0.50f, 0.50f, 1f)
                          : caughtJunk != null ? new Color(0.55f, 0.48f, 0.32f, 1f)
                          : RARITY_COL.getOrDefault(caughtFish.rarity(), Color.WHITE);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.55f);
        shapes.rect(0, 0, W, H);
        shapes.setColor(0.06f, 0.07f, 0.13f, 1f);
        shapes.rect(px, py, pw, ph);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(borderColor);
        shapes.rect(px, py, pw, ph);
        shapes.rect(px + 2f, py + 2f, pw - 4f, ph - 4f);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        float tx = px + 30f;
        float ty = py + ph - 28f;

        if (lastWasMiss) {
            font.setColor(0.60f, 0.60f, 0.60f, 1f);
            font.getData().setScale(1.7f);
            font.draw(batch, "It got away!", tx, ty);
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            font.draw(batch, missMessage, tx, ty - 34f);
        } else if (caughtJunk != null) {
            font.setColor(0.62f, 0.55f, 0.42f, 1f);
            font.getData().setScale(2.0f);
            font.draw(batch, caughtJunk.displayName, tx, ty);
            font.getData().setScale(1f);
            ty -= 44f;
            font.setColor(0.45f, 0.42f, 0.35f, 1f);
            font.draw(batch, "Junk", tx, ty);
            ty -= 26f;
            font.setColor(Color.WHITE);
            font.draw(batch, "1 coin", tx, ty);
            ty -= 22f;
            font.setColor(0.45f, 0.80f, 0.45f, 1f);
            font.draw(batch, "Added to bag  (" + game.gameState.getFishBag().size() + "/" + game.gameState.getFishBag().getMaxSlots() + ")", tx, ty);
            ty -= 28f;
            font.setColor(0.55f, 0.52f, 0.44f, 1f);
            font.draw(batch, "\"" + caughtJunk.description + "\"", tx, ty, pw - 60f, Align.left, true);
        } else {
            if (caughtShiny) {
                font.setColor(1f, 0.85f, 0.10f, 1f);
                font.getData().setScale(1.3f);
                font.draw(batch, "  SHINY!", tx, ty);
                font.getData().setScale(1f);
                ty -= 30f;
            }
            font.getData().setScale(2.0f);
            font.setColor(Color.WHITE);
            font.draw(batch, caughtFish.name(), tx, ty);
            font.getData().setScale(1f);
            ty -= 44f;
            font.setColor(borderColor);
            font.draw(batch, caughtFish.rarity().displayName, tx, ty);
            ty -= 26f;
            font.setColor(Color.WHITE);
            font.draw(batch, String.format("%.2f kg     %d coins", caughtWeight, caughtValue), tx, ty);
            ty -= 22f;
            font.setColor(0.45f, 0.80f, 0.45f, 1f);
            font.draw(batch, "Added to bag  (" + game.gameState.getFishBag().size() + "/" + game.gameState.getFishBag().getMaxSlots() + ")", tx, ty);
            ty -= 28f;
            font.setColor(0.72f, 0.72f, 0.72f, 1f);
            font.draw(batch, "\"" + caughtFish.description() + "\"", tx, ty, pw - 160f, Align.left, true);
        }

        if (caughtFish != null) {
            Map<String, Texture> src = caughtShiny ? shinyFishTextures : fishTextures;
            Texture ft = src.getOrDefault(caughtFish.id(), fishTextures.get(caughtFish.id()));
            if (ft != null) {
                float maxSide = 96f;
                float scale   = Math.min(maxSide / ft.getWidth(), maxSide / ft.getHeight());
                float iw = ft.getWidth() * scale, ih = ft.getHeight() * scale;
                batch.draw(ft, px + pw - iw - 20f, py + ph / 2f - ih / 2f, iw, ih);
            }
        } else if (caughtJunk != null) {
            Texture jt = junkTextures.get(caughtJunk);
            if (jt != null) {
                float maxSide = 96f;
                float scale   = Math.min(maxSide / jt.getWidth(), maxSide / jt.getHeight());
                float iw = jt.getWidth() * scale, ih = jt.getHeight() * scale;
                batch.draw(jt, px + pw - iw - 20f, py + ph / 2f - ih / 2f, iw, ih);
            }
        }

        font.setColor(0.55f, 0.55f, 0.55f, 1f);
        font.draw(batch, "[SPACE]  Continue", tx, py + 22f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void drawMinigame() {
        final float BAR_W = mgBarWidth;
        final float BAR_H = 36f;
        final float BAR_X = (W - BAR_W) / 2f;
        final float BAR_Y = H / 2f - 18f;
        final float PNL_W = 560f;
        final float PNL_H = 200f;
        final float PNL_X = (W - PNL_W) / 2f;
        final float PNL_Y = H / 2f - 100f;

        Color rc = RARITY_COL.getOrDefault(
            caughtFish != null ? caughtFish.rarity() : Rarity.RARE, Color.WHITE);

        boolean hookOnFish = Math.abs(0.5f - mgZonePos) <= mgZoneHalfWidth;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        shapes.setColor(0f, 0f, 0f, 0.68f);
        shapes.rect(0, 0, W, H);

        if (mgMissFlash > 0f) {
            shapes.setColor(0.9f, 0.15f, 0.1f, mgMissFlash * 0.35f);
            shapes.rect(0, 0, W, H);
        }

        shapes.setColor(0.06f, 0.07f, 0.13f, 1f);
        shapes.rect(PNL_X, PNL_Y, PNL_W, PNL_H);

        shapes.setColor(0.14f, 0.15f, 0.20f, 1f);
        shapes.rect(BAR_X, BAR_Y, BAR_W, BAR_H);

        float zoneX = BAR_X + (mgZonePos - mgZoneHalfWidth) * BAR_W;
        float zoneW = mgZoneHalfWidth * 2f * BAR_W;
        shapes.setColor(rc.r, rc.g, rc.b, 0.50f);
        shapes.rect(zoneX, BAR_Y, zoneW, BAR_H);

        float hookX = BAR_X + 0.5f * BAR_W;
        shapes.setColor(hookOnFish ? Color.GREEN : Color.WHITE);
        shapes.rectLine(hookX, BAR_Y, hookX, BAR_Y + BAR_H + 18f, 2.5f);
        shapes.triangle(
            hookX,      BAR_Y + BAR_H + 28f,
            hookX - 9f, BAR_Y + BAR_H + 44f,
            hookX + 9f, BAR_Y + BAR_H + 44f
        );

        if (mgAttempts > 0) {
            for (int i = 0; i < mgAttempts; i++) {
                shapes.setColor(rc.r, rc.g, rc.b, 1f);
                shapes.circle(PNL_X + 30f + i * 16f, PNL_Y + 52f, 5f, 12);
            }
        }

        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(rc);
        shapes.rect(PNL_X, PNL_Y, PNL_W, PNL_H);
        shapes.rect(PNL_X + 2f, PNL_Y + 2f, PNL_W - 4f, PNL_H - 4f);
        shapes.setColor(0.35f, 0.36f, 0.42f, 1f);
        shapes.rect(BAR_X, BAR_Y, BAR_W, BAR_H);
        shapes.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        String label = caughtFish != null ? caughtFish.rarity().displayName.toUpperCase() : "RARE";
        font.getData().setScale(1.8f);
        font.setColor(rc);
        font.draw(batch, label + " CATCH!", PNL_X + 30f, PNL_Y + PNL_H - 16f);
        font.getData().setScale(1f);

        font.setColor(Color.WHITE);
        font.draw(batch, "Wait for the fish to swim over the hook!", PNL_X + 30f, PNL_Y + PNL_H - 46f);

        float timeLeft = Math.max(0f, mgTimeLimit - mgTimeElapsed);
        font.setColor(timeLeft < 8f ? new Color(1f, 0.35f, 0.25f, 1f) : new Color(0.52f, 0.52f, 0.58f, 1f));
        font.draw(batch, String.format("%.1fs", timeLeft), PNL_X + PNL_W - 80f, PNL_Y + 62f);

        if (mgAttempts < 0) {
            font.setColor(0.52f, 0.52f, 0.58f, 1f);
            font.draw(batch, "Unlimited", PNL_X + 30f, PNL_Y + 62f);
        }

        font.setColor(0.50f, 0.50f, 0.55f, 1f);
        font.draw(batch, "[SPACE]  Strike when the fish is on the hook!", PNL_X + 30f, PNL_Y + 22f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void drawJournalPanel() {
        final float PW = 760f, PH = 560f;
        final float PX = (W - PW) / 2f, PY = (H - PH) / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.60f);
        shapes.rect(0, 0, W, H);
        shapes.setColor(0.06f, 0.07f, 0.13f, 1f);
        shapes.rect(PX, PY, PW, PH);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(COIN_GOLD);
        shapes.rect(PX, PY, PW, PH);
        shapes.rect(PX + 2f, PY + 2f, PW - 4f, PH - 4f);
        shapes.setColor(0.35f, 0.30f, 0.12f, 1f);
        shapes.line(PX + 12f, PY + PH - 46f, PX + PW - 12f, PY + PH - 46f);
        shapes.line(PX + 12f, PY + 38f,      PX + PW - 12f, PY + 38f);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        font.getData().setScale(1.8f);
        font.setColor(COIN_GOLD);
        font.draw(batch, "Journal", PX + 20f, PY + PH - 14f);
        font.getData().setScale(1f);

        font.setColor(journalTab == 0 ? Color.WHITE : new Color(0.45f, 0.45f, 0.52f, 1f));
        font.draw(batch, "[←] Encyclopedia", PX + 220f, PY + PH - 26f);
        font.setColor(journalTab == 1 ? Color.WHITE : new Color(0.45f, 0.45f, 0.52f, 1f));
        font.draw(batch, "Achievements [→]", PX + 460f, PY + PH - 26f);

        if (journalTab == 0) {
            drawEncyclopediaContent(PX, PY, PW, PH);
        } else {
            drawAchievementsContent(PX, PY, PW, PH);
        }

        font.setColor(0.55f, 0.55f, 0.55f, 1f);
        font.draw(batch, "[J]  Close      [←→]  Switch tab      [↑↓]  Scroll", PX + 20f, PY + 24f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void drawEncyclopediaContent(float PX, float PY, float PW, float PH) {
        List<Fish> allFish = FishRegistry.getAll();
        float rowH   = 38f;
        float rowTop = PY + PH - 58f;
        int   visEnd = Math.min(journalScrollOffset + JOURNAL_VISIBLE, allFish.size());

        long discovered = allFish.stream()
            .filter(f -> game.gameState.getFishRecord(f.id()).discovered)
            .count();
        font.setColor(0.52f, 0.52f, 0.58f, 1f);
        font.draw(batch, "Discovered: " + discovered + " / " + allFish.size(), PX + 20f, PY + 52f);

        for (int i = journalScrollOffset; i < visEnd; i++) {
            Fish fish = allFish.get(i);
            FishRecord rec = game.gameState.getFishRecord(fish.id());
            float ry = rowTop - (i - journalScrollOffset) * rowH;
            Color rc = RARITY_COL.getOrDefault(fish.rarity(), Color.WHITE);

            if (rec.discovered) {
                font.setColor(rc);
                font.draw(batch, "•", PX + 20f, ry);  // bullet
                font.setColor(Color.WHITE);
                font.draw(batch, fish.name(), PX + 48f, ry);
                font.setColor(0.58f, 0.58f, 0.64f, 1f);
                font.draw(batch, "x" + rec.totalCaught, PX + 310f, ry);
                if (rec.personalBest > 0) {
                    font.draw(batch, String.format("%.2f kg", rec.personalBest), PX + 430f, ry);
                }
                if (rec.shinyUnlocked) {
                    font.setColor(1f, 0.88f, 0.10f, 1f);
                    font.draw(batch, "★", PX + 640f, ry);  // black star
                }
            } else {
                font.setColor(rc.r * 0.35f, rc.g * 0.35f, rc.b * 0.35f, 1f);
                font.draw(batch, "•", PX + 20f, ry);
                font.setColor(0.25f, 0.25f, 0.30f, 1f);
                font.draw(batch, "???", PX + 48f, ry);
                font.setColor(0.20f, 0.20f, 0.25f, 1f);
                font.draw(batch, fish.rarity().displayName, PX + 310f, ry);
            }
        }

        font.setColor(0.45f, 0.45f, 0.50f, 1f);
        if (journalScrollOffset > 0)
            font.draw(batch, "↑ scroll", PX + PW - 100f, PY + PH - 50f);
        if (visEnd < allFish.size())
            font.draw(batch, "↓ scroll", PX + PW - 100f, PY + 52f);
    }

    private void drawAchievementsContent(float PX, float PY, float PW, float PH) {
        Achievement[] achs = Achievement.values();
        float rowH   = 38f;
        float rowTop = PY + PH - 58f;

        long unlocked = java.util.Arrays.stream(achs)
            .filter(a -> game.gameState.hasAchievement(a))
            .count();
        font.setColor(0.52f, 0.52f, 0.58f, 1f);
        font.draw(batch, "Unlocked: " + unlocked + " / " + achs.length, PX + 20f, PY + 52f);

        for (int i = 0; i < achs.length; i++) {
            Achievement a = achs[i];
            float ry = rowTop - i * rowH;
            boolean isUnlocked = game.gameState.hasAchievement(a);

            if (isUnlocked) {
                font.setColor(0.25f, 0.82f, 0.25f, 1f);
                font.draw(batch, "✓", PX + 20f, ry);  // check mark
                font.setColor(Color.WHITE);
                font.draw(batch, a.displayName, PX + 48f, ry);
                font.setColor(0.58f, 0.58f, 0.64f, 1f);
                font.draw(batch, a.description, PX + 300f, ry);
            } else {
                font.setColor(0.28f, 0.28f, 0.33f, 1f);
                font.draw(batch, "✗", PX + 20f, ry);  // cross mark
                font.setColor(0.38f, 0.38f, 0.44f, 1f);
                font.draw(batch, a.displayName, PX + 48f, ry);
                font.setColor(0.25f, 0.25f, 0.30f, 1f);
                font.draw(batch, a.description, PX + 300f, ry);
            }
        }
    }

    private void drawBagPanel() {
        float pw = 520f, ph = 500f;
        float px = (W - pw) / 2f, py = (H - ph) / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.60f);
        shapes.rect(0, 0, W, H);
        shapes.setColor(0.06f, 0.07f, 0.13f, 1f);
        shapes.rect(px, py, pw, ph);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(COIN_GOLD);
        shapes.rect(px, py, pw, ph);
        shapes.rect(px + 2f, py + 2f, pw - 4f, ph - 4f);
        shapes.setColor(0.35f, 0.30f, 0.12f, 1f);
        shapes.line(px + 12f, py + ph - 46f, px + pw - 12f, py + ph - 46f);
        shapes.line(px + 12f, py + 38f,      px + pw - 12f, py + 38f);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        FishBag bag = game.gameState.getFishBag();

        font.getData().setScale(1.8f);
        font.setColor(COIN_GOLD);
        font.draw(batch, "Fish Bag  (" + bag.size() + "/" + bag.getMaxSlots() + ")", px + 20f, py + ph - 14f);
        font.getData().setScale(1f);

        List<CaughtFish> slots = bag.getSlots();
        int maxSlots  = bag.getMaxSlots();
        int visEnd    = Math.min(bagScrollOffset + BAG_VISIBLE, maxSlots);
        for (int i = bagScrollOffset; i < visEnd; i++) {
            float ry = py + ph - 58f - (i - bagScrollOffset) * 36f;
            font.setColor(0.40f, 0.40f, 0.45f, 1f);
            font.draw(batch, (i + 1) + ".", px + 20f, ry);
            if (i < slots.size()) {
                CaughtFish c = slots.get(i);
                if (c.isJunk()) {
                    font.setColor(0.58f, 0.52f, 0.40f, 1f);
                    font.draw(batch, c.displayName(), px + 48f, ry);
                    font.setColor(COIN_GOLD);
                    font.draw(batch, c.value() + " coin", px + 370f, ry);
                } else {
                    font.setColor(RARITY_COL.getOrDefault(c.fish().rarity(), Color.WHITE));
                    font.draw(batch, c.fish().name(), px + 48f, ry);
                    font.setColor(0.78f, 0.78f, 0.78f, 1f);
                    font.draw(batch, String.format("%.2f kg", c.weight()), px + 262f, ry);
                    font.setColor(COIN_GOLD);
                    font.draw(batch, c.value() + " coins", px + 370f, ry);
                    if (c.shiny()) {
                        font.setColor(1f, 0.88f, 0.10f, 1f);
                        font.draw(batch, "[S]", px + 480f, ry);
                    }
                }
            } else {
                font.setColor(0.22f, 0.22f, 0.26f, 1f);
                font.draw(batch, "---", px + 48f, ry);
            }
        }

        font.setColor(0.55f, 0.55f, 0.55f, 1f);
        if (bagScrollOffset > 0)
            font.draw(batch, "↑ scroll", px + pw - 100f, py + ph - 50f);
        if (visEnd < slots.size())
            font.draw(batch, "↓ scroll", px + pw - 100f, py + 52f);
        font.draw(batch, "[B]  Close      [↑↓]  Scroll", px + 20f, py + 24f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void drawDevWindow() {
        float pw = 760f, ph = 360f;
        float px = (W - pw) / 2f, py = (H - ph) / 2f;
        float col3 = pw / 3f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.70f);
        shapes.rect(0, 0, W, H);
        shapes.setColor(0.06f, 0.07f, 0.13f, 1f);
        shapes.rect(px, py, pw, ph);
        shapes.setColor(0.15f, 0.15f, 0.22f, 1f);
        shapes.rect(px + col3 - 1f,       py + 10f, 2f, ph - 20f);
        shapes.rect(px + col3 * 2f - 1f,  py + 10f, 2f, ph - 20f);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(COIN_GOLD);
        shapes.rect(px, py, pw, ph);
        shapes.rect(px + 2f, py + 2f, pw - 4f, ph - 4f);
        shapes.setColor(0.35f, 0.30f, 0.12f, 1f);
        shapes.line(px + 12f, py + ph - 46f, px + pw - 12f, py + ph - 46f);
        shapes.line(px + 12f, py + 38f,      px + pw - 12f, py + 38f);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        font.getData().setScale(1.8f);
        font.setColor(COIN_GOLD);
        font.draw(batch, "Dev Options", px + 20f, py + ph - 14f);
        font.getData().setScale(1f);

        TimeOfDay[] periods  = TimeOfDay.values();
        Weather[]   weathers = Weather.values();
        float colL = px + 20f;
        float colM = px + col3 + 20f;
        float colR = px + col3 * 2f + 20f;
        float rowTop = py + ph - 58f;
        float rowH   = 34f;

        Color hdrCol = devSection == 0 ? COIN_GOLD : new Color(0.55f, 0.55f, 0.55f, 1f);
        font.setColor(hdrCol);
        font.draw(batch, "Time of Day", colL, rowTop);
        for (int i = 0; i < periods.length; i++) {
            float ry = rowTop - rowH - i * rowH;
            boolean selected = i == devTimeCursor;
            boolean active   = devSection == 0 && selected;
            font.setColor(active ? Color.WHITE : (selected ? new Color(0.75f, 0.75f, 0.75f, 1f) : new Color(0.45f, 0.45f, 0.50f, 1f)));
            font.draw(batch, (active ? "> " : "  ") + periods[i].displayName, colL, ry);
        }

        Color hdrColW = devSection == 1 ? COIN_GOLD : new Color(0.55f, 0.55f, 0.55f, 1f);
        font.setColor(hdrColW);
        font.draw(batch, "Weather", colM, rowTop);
        for (int i = 0; i < weathers.length; i++) {
            float ry = rowTop - rowH - i * rowH;
            boolean selected = i == devWeatherCursor;
            boolean active   = devSection == 1 && selected;
            font.setColor(active ? Color.WHITE : (selected ? new Color(0.75f, 0.75f, 0.75f, 1f) : new Color(0.45f, 0.45f, 0.50f, 1f)));
            font.draw(batch, (active ? "> " : "  ") + weathers[i].displayName, colM, ry);
        }

        Color hdrColM = devSection == 2 ? COIN_GOLD : new Color(0.55f, 0.55f, 0.55f, 1f);
        font.setColor(hdrColM);
        font.draw(batch, "Give Money", colR, rowTop);
        for (int i = 0; i < DEV_MONEY_AMOUNTS.length; i++) {
            float ry = rowTop - rowH - i * rowH;
            boolean selected = i == devMoneyCursor;
            boolean active   = devSection == 2 && selected;
            font.setColor(active ? Color.WHITE : (selected ? new Color(0.75f, 0.75f, 0.75f, 1f) : new Color(0.45f, 0.45f, 0.50f, 1f)));
            font.draw(batch, (active ? "> " : "  ") + "+" + DEV_MONEY_AMOUNTS[i] + " coins", colR, ry);
        }

        font.setColor(0.55f, 0.55f, 0.55f, 1f);
        font.draw(batch, "[F1]  Close    [←→]  Switch    [↑↓]  Select    [ENTER]  Apply", px + 20f, py + 24f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void updateWaves(float delta) {
        waveTime      += delta;
        waveEventTimer += delta;
        if (waveEventTimer >= nextWaveEvent) {
            waveEventTimer    = 0f;
            nextWaveEvent     = 20f + rng.nextFloat() * 15f;
            waveEventIntensity = 1f;
            waveSound.play(0.38f);
        }
        if (waveEventIntensity > 0f) {
            waveEventIntensity = Math.max(0f, waveEventIntensity - delta * 0.45f);
        }
    }

    private void drawWaves() {
        for (int layer = 0; layer < 3; layer++) {
            float speed = 0.35f + layer * 0.18f;
            float freq  = 0.009f + layer * 0.003f;
            float amp   = (3f + layer * 2f) * (1f + waveEventIntensity * 1.8f);
            float yBase = hz - 90f - layer * 14f;
            float alpha = (0.22f - layer * 0.04f) + waveEventIntensity * 0.28f;

            float r = Math.min(1f, currentWater.r * 0.4f + 0.65f);
            float g = Math.min(1f, currentWater.g * 0.4f + 0.60f);
            float b = Math.min(1f, currentWater.b * 0.3f + 0.72f);
            shapes.setColor(r, g, b, alpha);

            for (float x = 0; x < W; x += 14f) {
                float y = yBase + MathUtils.sin(x * freq + waveTime * speed) * amp;
                shapes.rect(x, y, 12f, 2.5f);
            }
        }
    }

    private void updateShimmer(float delta) {
        shimmerTime += delta;
        for (int i = 0; i < SHIMMER_COUNT; i++) {
            shimmerAge[i] += delta;
            if (shimmerAge[i] >= shimmerLife[i]) {
                shimmerX[i]    = 730f + rng.nextFloat() * 530f;
                shimmerY[i]    = 20f  + rng.nextFloat() * (hz - 40f);
                shimmerLife[i] = 0.4f + rng.nextFloat() * 1.3f;
                shimmerAge[i]  = 0f;
            }
        }
    }

    private void drawWaterShimmer() {
        float wr = currentWater.r, wg = currentWater.g, wb = currentWater.b;

        for (int i = 0; i < SHIMMER_COUNT; i++) {
            float t     = shimmerAge[i] / shimmerLife[i];
            float alpha = (t < 0.25f ? t / 0.25f : (1f - t) / 0.75f) * 0.30f;
            shapes.setColor(
                Math.min(1f, wr + 0.45f),
                Math.min(1f, wg + 0.38f),
                Math.min(1f, wb + 0.28f),
                alpha);
            shapes.rect(shimmerX[i], shimmerY[i], 3f, 2f);
        }

        for (int i = 0; i < 7; i++) {
            float y = 55f  + i * 35f + MathUtils.sin(shimmerTime * 0.25f + i * 1.7f) * 8f;
            float x = 960f + MathUtils.sin(shimmerTime * 0.18f + i * 1.5f) * 220f;
            float w = 55f  + MathUtils.sin(shimmerTime * 0.40f + i * 2.3f) * 22f;
            float a = 0.05f + MathUtils.sin(shimmerTime * 0.50f + i * 1.1f) * 0.02f;
            shapes.setColor(
                Math.min(1f, wr + 0.52f),
                Math.min(1f, wg + 0.45f),
                Math.min(1f, wb + 0.32f),
                a);
            shapes.rect(x, y, w, 2f);
        }
    }

    private void triggerSplash(float x, float y) {
        splashCentreX = x;
        splashCentreY = y;
        splashTimer   = 0f;
        for (int i = 0; i < SPLASH_COUNT; i++) {
            float angle = MathUtils.PI * (0.15f + 0.70f * i / (SPLASH_COUNT - 1));
            float speed = 65f + rng.nextFloat() * 75f;
            splashX[i]  = x;
            splashY[i]  = y;
            splashVX[i] = MathUtils.cos(angle) * speed;
            splashVY[i] = MathUtils.sin(angle) * speed;
        }
    }

    private void updateSplash(float delta) {
        if (splashTimer < 0f) return;
        splashTimer += delta;
        for (int i = 0; i < SPLASH_COUNT; i++) {
            splashX[i]  += splashVX[i] * delta;
            splashY[i]  += splashVY[i] * delta;
            splashVY[i] += SPLASH_GRAVITY * delta;
        }
    }

    private void drawSplash() {
        if (splashTimer < 0f || splashTimer > SPLASH_DURATION) return;
        float t  = splashTimer / SPLASH_DURATION;
        float wr = currentWater.r, wg = currentWater.g, wb = currentWater.b;

        float ringR     = splashTimer * 55f;
        float ringAlpha = (1f - t) * 0.55f;
        shapes.setColor(Math.min(1f, wr + 0.45f), Math.min(1f, wg + 0.40f), Math.min(1f, wb + 0.30f), ringAlpha);
        for (int p = 0; p < 20; p++) {
            float a = p * MathUtils.PI2 / 20f;
            shapes.rect(splashCentreX + MathUtils.cos(a) * ringR - 1.5f,
                        splashCentreY + MathUtils.sin(a) * ringR - 1.5f, 3f, 3f);
        }

        float dropAlpha = (1f - t) * 0.90f;
        shapes.setColor(Math.min(1f, wr + 0.52f), Math.min(1f, wg + 0.46f), Math.min(1f, wb + 0.36f), dropAlpha);
        for (int i = 0; i < SPLASH_COUNT; i++) {
            shapes.rect(splashX[i] - 1.5f, splashY[i] - 1.5f, 3f, 3f);
        }
    }

    private void updateWeather(Weather weather, float delta) {
        fogTime += delta;
        if (weather == Weather.RAIN || weather == Weather.STORM) {
            float speed = weather == Weather.STORM ? 620f : 400f;
            for (int i = 0; i < RAIN_COUNT; i++) {
                rainY[i] -= speed * delta;
                rainX[i] -= speed * 0.22f * delta;
                if (rainY[i] < -20f) {
                    rainY[i] = H + rng.nextFloat() * 80f;
                    rainX[i] = rng.nextFloat() * (W + 80f);
                }
            }
        }
    }

    private void drawWeatherEffects(Weather weather) {
        switch (weather) {
            case FOG   -> drawFog(0.20f);
            case RAIN  -> drawRain(false);
            case STORM -> { drawRain(true); drawFog(0.12f); drawLightningBolt(); }
            default    -> {}
        }
    }

    private void triggerLightning() {
        lightningFlash = 1f;
        boltPoints = 7 + rng.nextInt(3);
        boltX[0] = 120f + rng.nextFloat() * (W - 240f);
        boltY[0] = H;
        for (int i = 1; i < boltPoints; i++) {
            boltX[i] = boltX[i - 1] + (rng.nextFloat() - 0.5f) * 90f;
            boltY[i] = H - (float) i / (boltPoints - 1) * (H - hz);
        }
    }

    private void drawLightningBolt() {
        if (lightningFlash <= 0f) return;
        shapes.setColor(0.85f, 0.90f, 1f, lightningFlash * 0.9f);
        for (int i = 0; i < boltPoints - 1; i++)
            shapes.rectLine(boltX[i], boltY[i], boltX[i + 1], boltY[i + 1], 3f);
        shapes.setColor(1f, 1f, 1f, lightningFlash);
        for (int i = 0; i < boltPoints - 1; i++)
            shapes.rectLine(boltX[i], boltY[i], boltX[i + 1], boltY[i + 1], 1.2f);
    }

    private void drawLightningFlash() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.88f, 0.93f, 1f, lightningFlash * 0.30f);
        shapes.rect(0, 0, W, H);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawRain(boolean storm) {
        int   count = storm ? RAIN_COUNT : RAIN_COUNT * 2 / 3;
        float alpha = storm ? 0.60f : 0.40f;
        shapes.setColor(0.65f, 0.73f, 0.90f, alpha);
        for (int i = 0; i < count; i++) {
            shapes.rectLine(rainX[i], rainY[i],
                            rainX[i] - 4f, rainY[i] - 16f, 1.2f);
        }
    }

    private void drawFog(float baseAlpha) {
        shapes.setColor(0.78f, 0.82f, 0.86f, baseAlpha * 0.7f);
        shapes.rect(0, hz, W, H - hz);

        float[] yOff    = { -30f,  25f,  70f, 120f, -80f };
        float[] alphas  = { 0.22f, 0.18f, 0.14f, 0.10f, 0.16f };
        float[] speeds  = {  14f,  -9f,  18f,   -6f,  11f };
        float[] heights = {  55f,  45f,  70f,   40f,  35f };
        for (int i = 0; i < 5; i++) {
            float a = alphas[i] * (baseAlpha / 0.20f);
            float x = ((fogTime * speeds[i]) % W + W) % W;
            shapes.setColor(0.80f, 0.84f, 0.87f, a);
            shapes.rect(x - W, hz + yOff[i], W, heights[i]);
            shapes.rect(x,     hz + yOff[i], W, heights[i]);
        }
    }

    private void updateAmbience(float delta) {
        ambienceTimer += delta;
        if (ambienceTimer >= nextAmbienceTime) {
            waterSounds[rng.nextInt(waterSounds.length)].play(0.45f);
            ambienceTimer    = 0f;
            nextAmbienceTime = 12f + rng.nextFloat() * 18f;
        }
    }

    private void lerpPalette(Color[] palette, TimeOfDay time, Color dest) {
        int   cur  = time.ordinal();
        int   next = (cur + 1) % TimeOfDay.values().length;
        float t    = periodProgress(time);
        dest.set(palette[cur]).lerp(palette[next], t);
    }

    private float periodProgress(TimeOfDay period) {
        int m = clock.getMinuteOfDay();
        int s = period.startHour * 60;
        int e = period.endHour   * 60;
        int duration, elapsed;
        if (s < e) {
            duration = e - s;
            elapsed  = m - s;
        } else {
            duration = (1440 - s) + e;
            elapsed  = m >= s ? m - s : (1440 - s) + m;
        }
        return MathUtils.clamp((float) elapsed / duration, 0f, 1f);
    }

    public int getClockMinute() { return clock.getMinuteOfDay(); }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, W, H);
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        for (Texture t : bgDayClear)   t.dispose();
        for (Texture t : bgDayOvercast) t.dispose();
        for (Texture t : bgDayRain)    t.dispose();
        for (Texture t : bgNight)         t.dispose();
        for (Texture t : bgNightOvercast) t.dispose();
        for (Sound s : waterSounds) s.dispose();
        castSound.dispose();
        lureBobSound.dispose();
        fishBiteSound.dispose();
        bubblesSound.dispose();
        waveSound.dispose();
        texFisherStanding.dispose();
        texFisherSitting.dispose();
        texRodBasic.dispose();
        texRodAdvanced.dispose();
        texRodMaster.dispose();
        for (Texture t : texBobber) t.dispose();
        for (Texture t : fishTextures.values()) t.dispose();
        for (Texture t : shinyFishTextures.values()) t.dispose();
        for (Texture t : junkTextures.values()) t.dispose();
    }
}
