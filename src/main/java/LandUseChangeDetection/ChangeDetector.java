package LandUseChangeDetection;

public class ChangeDetector {

    private SentinelData beforeSentinelData;

    private SentinelData afterSentinelData;

    public ChangeDetector(SentinelData beforeSentinelData, SentinelData afterSentinelData) {
        // TODO: Проверка дат
        this.beforeSentinelData = beforeSentinelData;
        this.afterSentinelData = afterSentinelData;

        cropScenes();
    }

    private void cropScenes() {
        if (beforeSentinelData == null || afterSentinelData == null) {
            return;
        }
        beforeSentinelData.cropBands(afterSentinelData.getEnvelope());
        afterSentinelData.cropBands(beforeSentinelData.getEnvelope());
    }

    public void getChanges() {
        if (beforeSentinelData.getHeight() != afterSentinelData.getHeight()
                || beforeSentinelData.getWidth() != afterSentinelData.getWidth()) {
            return;
        }
        Classification svm = Classification.getInstance();
        int pixelsCount = beforeSentinelData.getHeight() * beforeSentinelData.getWidth();
        for (int i = 0; i < pixelsCount; ++i) {
            double[] first = beforeSentinelData.getPixelVector(i);
            double[] second = afterSentinelData.getPixelVector(i);
            int firstClass = svm.predict(first);
            int secondClass = svm.predict(second);
            if (firstClass == secondClass) {
                System.out.println("OK " + firstClass + " " + secondClass);
            } else {
                System.out.println("Changed " + first + " to " + secondClass);
            }
        }

    }
}
