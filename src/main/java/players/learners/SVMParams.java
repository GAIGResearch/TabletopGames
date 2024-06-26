package players.learners;

import evaluation.optimisation.TunableParameters;
import libsvm.svm_parameter;

import static players.learners.SVMParams.SVM_CONSTANTS.NU_SVR;
import static players.learners.SVMParams.SVM_CONSTANTS.RBF;

public class SVMParams extends TunableParameters {

    SVM_CONSTANTS kernel;
    SVM_CONSTANTS type;
    double nu, C, eps, p, gamma, coef0;
    int degree;
    public SVMParams() {
        addTunableParameter("kernel", RBF);
        addTunableParameter("type", NU_SVR);
        addTunableParameter("nu", 0.5);
        addTunableParameter("C", 1.0);
        addTunableParameter("eps", 0.001);  // termination condition epsilon
        addTunableParameter("p", 0.1); // epsilon in E-SVR
        addTunableParameter("degree", 3); // used in polynomial kernel
        addTunableParameter("gamma", 1.0); // used in RBF kernel
        addTunableParameter("coef0", 1.0); // used in polynomial and sigmoid kernels
    }

    @Override
    public void _reset() {
        kernel = (SVM_CONSTANTS) getParameterValue("kernel");
        type = (SVM_CONSTANTS) getParameterValue("type");
        nu = (double) getParameterValue("nu");
        C = (double) getParameterValue("C");
        eps = (double) getParameterValue("eps");
        p = (double) getParameterValue("p");
        gamma = (double) getParameterValue("gamma");
        coef0 = (double) getParameterValue("coef0");
        degree = (int) getParameterValue("degree");
    }

    @Override
    protected SVMParams _copy() {
        SVMParams retValue = new SVMParams();
        retValue.kernel = kernel;
        retValue.type = type;
        retValue.nu = nu;
        retValue.C = C;
        retValue.eps = eps;
        retValue.p = p;
        retValue.gamma = gamma;
        retValue.coef0 = coef0;
        retValue.degree = degree;
        return retValue;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof SVMParams) {
            SVMParams other = (SVMParams) o;
            return other.kernel == kernel && other.type == type && other.nu == nu && other.C == C
                    && other.eps == eps && other.p == p && other.gamma == gamma
                    && other.coef0 == coef0 && other.degree == degree;
        }
        return false;
    }

    @Override
    public svm_parameter instantiate() {
        svm_parameter retValue = new svm_parameter();
        retValue.kernel_type = kernel.constant;
        retValue.svm_type = type.constant;
        retValue.nu = nu;
        retValue.C = C;
        retValue.eps = eps;
        retValue.p = p;
        retValue.gamma = gamma;
        retValue.coef0 = coef0;
        retValue.degree = degree;
        return retValue;
    }

    public enum SVM_CONSTANTS {
        C_CVC(0), NU_SVC(1), EPSILON_SVR(3), NU_SVR(4), RBF(2), POLY(1), LINEAR(0), SIGMOID(3);

        public final int constant;

        SVM_CONSTANTS(int ref) {
            constant = ref;
        }
    }


}
