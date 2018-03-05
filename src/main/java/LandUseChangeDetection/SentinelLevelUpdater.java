package LandUseChangeDetection;

import LandUseChangeDetection.forms.LevelUpForm;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SentinelLevelUpdater {

    /**
     * Sen2Cor semaphore
     */
    private static final Semaphore SEMAPHORE = new Semaphore(1, true);

    /**
     * Level Up form
     */
    private LevelUpForm form;

    /**
     * Updater constructor
     * @param form level up form
     */
    public SentinelLevelUpdater(LevelUpForm form) {
        this.form = form;
    }

    /**
     * Level up Sentinel 2 data
     * @param sentinel1CLevel Sentinel 2 data directory
     * @throws IOException Ability to call to Sen2Cor
     * @throws InterruptedException Semaphore checking
     */
    public Process levelUp(File sentinel1CLevel) throws IOException, InterruptedException {
        Process process = upTo2ALevel(sentinel1CLevel);
        return process;
    }

    /**
     * Update Sentinel 1C level to 2A level
     * @param sentinel1CLevel Sentinel 2 data directory
     * @throws IOException Ability to call to Sen2Cor
     * @throws InterruptedException Semaphore checking
     */
    private Process upTo2ALevel(File sentinel1CLevel) throws IOException, InterruptedException {
        SEMAPHORE.acquire();
        // TODO: Change to Linux
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/C", "start", "/B",
                "L2A_Process", sentinel1CLevel.getAbsolutePath());
        Map<String, String> env = pb.environment();
        Map<String, String> sEnv = System.getenv();
        for (Map.Entry<String, String> entry : sEnv.entrySet()) {
            env.put(entry.getKey(), entry.getValue());
        }
        pb.redirectErrorStream(true);
        final Process process = pb.start();
        readRuntime(process);
        return process;
    }

    /**
     * Chang status
     * @param process process
     */
    private void readRuntime(final Process process) {
        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "cp866"));
                String line;
                form.progressBar.setProgress(0.0);
                Pattern pattern = Pattern.compile("Progress[%]: (\\d*\\.\\d*) : (.*)");
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    // TODO: Update
                }
                form.finishProcess();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                SEMAPHORE.release();
            }
        });
    }
}
