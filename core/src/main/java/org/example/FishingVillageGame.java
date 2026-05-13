package org.example;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import org.example.game.GameState;
import org.example.save.SaveManager;
import org.example.screen.FishingScreen;
import org.example.screen.ShopScreen;

public class FishingVillageGame extends Game {

    public final GameState gameState = new GameState();

    public FishingScreen fishingScreen;
    public ShopScreen    shopScreen;
    public Music         backgroundMusic;
    public boolean       musicMuted = false;

    @Override
    public void create() {
        Fonts.load();

        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("background_theme.ogg"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.3f);
        backgroundMusic.play();

        SaveManager.load(gameState);
        fishingScreen = new FishingScreen(this);
        shopScreen    = new ShopScreen(this);
        setScreen(fishingScreen);
    }

    public void toggleMute() {
        musicMuted = !musicMuted;
        backgroundMusic.setVolume(musicMuted ? 0f : 0.3f);
    }

    @Override
    public void pause() {
        gameState.setClockMinute(fishingScreen.getClockMinute());
        SaveManager.save(gameState);
    }

    @Override
    public void dispose() {
        gameState.setClockMinute(fishingScreen.getClockMinute());
        SaveManager.save(gameState);
        backgroundMusic.dispose();
        fishingScreen.dispose();
        shopScreen.dispose();
        Fonts.dispose();
    }
}
