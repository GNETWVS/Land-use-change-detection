package landUseChangeDetection;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class SentinelLevelUpdater {

    /**
     * Sen2Cor semaphore
     */
    public static final Semaphore SEMAPHORE = new Semaphore(1, true);

    /**
     * Update progress
     */
    private Process process;

    /**
     * Progress getter
     * @return Update process
     */
    public Process getProcess() {
        return this.process;
    }

    /**
     * Level up Sentinel 2 data
     * @param sentinel1CLevel Sentinel 2 data directory
     * @param param Sentinel data resolutions
     * @throws IOException Ability to call to Sen2Cor
     * @throws InterruptedException Semaphore checking
     */
    public void levelUp(File sentinel1CLevel, String param) throws IOException, InterruptedException {
        this.process = upTo2ALevel(sentinel1CLevel, param);
    }

    /**
     * Update Sentinel 1C level to 2A level
     * @param sentinel1CLevel Sentinel 2 data directory
     * @param param Sentinel data resolutions
     * @throws IOException Ability to call to Sen2Cor
     * @throws InterruptedException Semaphore checking
     */
    private Process upTo2ALevel(File sentinel1CLevel, String param) throws IOException, InterruptedException {
        SEMAPHORE.acquire();
        // TODO: Change to Linux
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/C", "start", "/B",
                "L2A_Process", sentinel1CLevel.getAbsolutePath(), param);

        Map<String, String> env = pb.environment();
        Map<String, String> sEnv = System.getenv();
        for (Map.Entry<String, String> entry : sEnv.entrySet()) {
            env.put(entry.getKey(), entry.getValue());
        }
        pb.redirectErrorStream(true);
        return process = pb.start();
    }
}
