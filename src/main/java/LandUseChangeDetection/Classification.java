package LandUseChangeDetection;

import smile.classification.SVM;
import smile.data.AttributeDataset;
import smile.math.kernel.GaussianKernel;

import java.io.File;
import java.nio.file.Path;

public class Classification {

    private SVM<double[]> svm;

    public Classification() {
        // TODO: Разобраться с sigma и penalty
        this.svm = new SVM<double[]>(new GaussianKernel(1), 1, 10, SVM.Multiclass.ONE_VS_ONE);
    }

    void learn(double[][] data, int[] label) {
        svm.learn(data, label);
        svm.finish();
    }

    void getOSMTrainingSamples(File osmShp) {

    }

    void getNextGISTrainingSamples(File nextShp) {

    }
}
