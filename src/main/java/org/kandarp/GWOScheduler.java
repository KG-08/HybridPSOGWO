package org.kandarp;

import org.cloudsimplus.cloudlets.Cloudlet;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class GWOScheduler {

    private final int numTasks;
    private final int numVMs;
    private final int populationSize;
    private final int iterations;

    private final double[] taskLengths;

    private double[][] positions;

    private double[] alpha;
    private double[] beta;
    private double[] delta;

    private double alphaFitness = Double.MAX_VALUE;
    private double betaFitness = Double.MAX_VALUE;
    private double deltaFitness = Double.MAX_VALUE;

    private final Random random = new Random(456);

    public GWOScheduler(
            List<Cloudlet> cloudlets,
            int numVMs,
            int populationSize,
            int iterations) {

        this.numTasks = cloudlets.size();
        this.numVMs = numVMs;
        this.populationSize = populationSize;
        this.iterations = iterations;

        this.taskLengths = new double[numTasks];

        for (int i = 0; i < numTasks; i++) {
            taskLengths[i] = cloudlets.get(i).getLength();
        }

        positions =
                new double[populationSize][numTasks];

        alpha = new double[numTasks];
        beta = new double[numTasks];
        delta = new double[numTasks];

        initialize();
    }

    private void initialize() {

        for (int i = 0; i < populationSize; i++) {

            for (int j = 0; j < numTasks; j++) {

                positions[i][j] =
                        random.nextDouble() * (numVMs - 1);
            }
        }

        for (int i = 0; i < populationSize; i++) {

            double fitness =
                    calculateFitness(positions[i]);

            updateLeaders(
                    positions[i],
                    fitness
            );
        }
    }

    public int[] optimize() {

        for (int iteration = 0;
             iteration < iterations;
             iteration++) {

            double a =
                    2.0 -
                            (2.0 * iteration /
                                    (iterations - 1));

            for (int i = 0;
                 i < populationSize;
                 i++) {

                double[] candidate =
                        new double[numTasks];

                for (int j = 0;
                     j < numTasks;
                     j++) {

                    double r1 =
                            random.nextDouble();

                    double r2 =
                            random.nextDouble();

                    double A1 =
                            2 * a * r1 - a;

                    double C1 =
                            2 * r2;

                    double D_alpha =
                            Math.abs(
                                    C1 * alpha[j]
                                            - positions[i][j]
                            );

                    double X1 =
                            alpha[j]
                                    - A1 * D_alpha;

                    r1 =
                            random.nextDouble();

                    r2 =
                            random.nextDouble();

                    double A2 =
                            2 * a * r1 - a;

                    double C2 =
                            2 * r2;

                    double D_beta =
                            Math.abs(
                                    C2 * beta[j]
                                            - positions[i][j]
                            );

                    double X2 =
                            beta[j]
                                    - A2 * D_beta;

                    r1 =
                            random.nextDouble();

                    r2 =
                            random.nextDouble();

                    double A3 =
                            2 * a * r1 - a;

                    double C3 =
                            2 * r2;

                    double D_delta =
                            Math.abs(
                                    C3 * delta[j]
                                            - positions[i][j]
                            );

                    double X3 =
                            delta[j]
                                    - A3 * D_delta;

                    candidate[j] =
                            (X1 + X2 + X3) / 3.0;

                    if (candidate[j] < 0) {
                        candidate[j] = 0;
                    }

                    if (candidate[j] > numVMs - 1) {
                        candidate[j] = numVMs - 1;
                    }
                }

                positions[i] = candidate;
            }

            alphaFitness = Double.MAX_VALUE;
            betaFitness = Double.MAX_VALUE;
            deltaFitness = Double.MAX_VALUE;

            for (int i = 0;
                 i < populationSize;
                 i++) {

                double fitness =
                        calculateFitness(
                                positions[i]
                        );

                updateLeaders(
                        positions[i],
                        fitness
                );
            }

            System.out.printf(
                    "GWO Iteration %d | Best Fitness: %.2f%n",
                    iteration + 1,
                    alphaFitness
            );
        }

        return convertToMapping(alpha);
    }

    private void updateLeaders(
            double[] wolf,
            double fitness) {

        if (fitness < alphaFitness) {

            deltaFitness = betaFitness;
            delta = beta.clone();

            betaFitness = alphaFitness;
            beta = alpha.clone();

            alphaFitness = fitness;
            alpha = wolf.clone();

        } else if (fitness < betaFitness) {

            deltaFitness = betaFitness;
            delta = beta.clone();

            betaFitness = fitness;
            beta = wolf.clone();

        } else if (fitness < deltaFitness) {

            deltaFitness = fitness;
            delta = wolf.clone();
        }
    }

    private double calculateFitness(
            double[] solution) {

        double[] vmWorkload =
                new double[numVMs];

        for (int task = 0;
             task < numTasks;
             task++) {

            int vm =
                    (int) Math.round(
                            solution[task]
                    );

            if (vm < 0) {
                vm = 0;
            }

            if (vm >= numVMs) {
                vm = numVMs - 1;
            }

            vmWorkload[vm] +=
                    taskLengths[task];
        }

        double makespan = 0;

        for (double workload :
                vmWorkload) {

            if (workload > makespan) {
                makespan = workload;
            }
        }

        return makespan;
    }

    private int[] convertToMapping(
            double[] solution) {

        int[] mapping =
                new int[numTasks];

        for (int i = 0;
             i < numTasks;
             i++) {

            int vm =
                    (int) Math.round(
                            solution[i]
                    );

            if (vm < 0) {
                vm = 0;
            }

            if (vm >= numVMs) {
                vm = numVMs - 1;
            }

            mapping[i] = vm;
        }

        return mapping;
    }

    public double[] getVmWorkloads() {

        double[] vmWorkload =
                new double[numVMs];

        for (int task = 0;
             task < numTasks;
             task++) {

            int vm =
                    (int) Math.round(
                            alpha[task]
                    );

            if (vm < 0) {
                vm = 0;
            }

            if (vm >= numVMs) {
                vm = numVMs - 1;
            }

            vmWorkload[vm] +=
                    taskLengths[task];
        }

        return vmWorkload;
    }

    public double getBestFitness() {
        return alphaFitness;
    }
}