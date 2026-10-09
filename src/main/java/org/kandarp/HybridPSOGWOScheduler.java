
package org.kandarp;

import org.cloudsimplus.cloudlets.Cloudlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class HybridPSOGWOScheduler {

    private final int numTasks;
    private final int numVMs;
    private final int populationSize;
    private final int iterations;

    private final double[] taskLengths;

    private int[][] positions;
    private double[][] velocities;
    private double[][] gwoPositions;

    private int[][] personalBest;
    private double[] personalBestFitness;

    private int[] globalBest;
    private double globalBestFitness = Double.MAX_VALUE;

    private final Random random = new Random(789);

    private final List<Double> lambdaHistory = new ArrayList<>();

    public HybridPSOGWOScheduler(
            List<Cloudlet> cloudlets,
            int numVMs,
            int populationSize,
            int iterations) {

        if (numVMs < 1 || populationSize < 1 || iterations < 2) {
            throw new IllegalArgumentException(
                    "VMs and population must be positive; iterations must be at least 2."
            );
        }

        this.numTasks = cloudlets.size();
        this.numVMs = numVMs;
        this.populationSize = populationSize;
        this.iterations = iterations;

        this.taskLengths = new double[numTasks];

        for (int i = 0; i < numTasks; i++) {
            taskLengths[i] = cloudlets.get(i).getLength();
        }

        positions = new int[populationSize][numTasks];
        velocities = new double[populationSize][numTasks];
        gwoPositions = new double[populationSize][numTasks];
        personalBest = new int[populationSize][numTasks];
        personalBestFitness = new double[populationSize];
        globalBest = new int[numTasks];

        initialize();
    }

    private void initialize() {
        for (int i = 0; i < populationSize; i++) {
            for (int j = 0; j < numTasks; j++) {
                positions[i][j] = random.nextInt(numVMs);
                velocities[i][j] = random.nextDouble() * 2 - 1;
                gwoPositions[i][j] = positions[i][j];
            }

            personalBest[i] = Arrays.copyOf(positions[i], numTasks);
            personalBestFitness[i] = calculateFitness(positions[i]);

            if (personalBestFitness[i] < globalBestFitness) {
                globalBestFitness = personalBestFitness[i];
                globalBest = Arrays.copyOf(positions[i], numTasks);
            }
        }
    }

    public int[] optimize() {
        double inertia = 0.7;
        double c1 = 1.5;
        double c2 = 1.5;

        lambdaHistory.clear();

        for (int iteration = 0; iteration < iterations; iteration++) {
            double a = 2.0 - (2.0 * iteration / (iterations - 1.0));

            double lambda = 0.9
                    - 0.5 * ((double) iteration / (iterations - 1));

            // Record the exact lambda used in this optimization iteration.
            lambdaHistory.add(lambda);

            updateGWO(a);

            for (int i = 0; i < populationSize; i++) {
                for (int j = 0; j < numTasks; j++) {
                    double r1 = random.nextDouble();
                    double r2 = random.nextDouble();

                    velocities[i][j] =
                            inertia * velocities[i][j]
                                    + c1 * r1
                                    * (personalBest[i][j] - positions[i][j])
                                    + c2 * r2
                                    * (globalBest[j] - positions[i][j]);

                    double psoPosition = positions[i][j] + velocities[i][j];

                    if (psoPosition < 0) {
                        psoPosition = 0;
                    }

                    if (psoPosition > numVMs - 1) {
                        psoPosition = numVMs - 1;
                    }

                    double gwoPosition = gwoPositions[i][j];

                    double hybridPosition =
                            lambda * gwoPosition
                                    + (1 - lambda) * psoPosition;

                    if (hybridPosition < 0) {
                        hybridPosition = 0;
                    }

                    if (hybridPosition > numVMs - 1) {
                        hybridPosition = numVMs - 1;
                    }

                    positions[i][j] = (int) Math.round(hybridPosition);
                }

                double fitness = calculateFitness(positions[i]);

                if (fitness < personalBestFitness[i]) {
                    personalBestFitness[i] = fitness;
                    personalBest[i] = Arrays.copyOf(positions[i], numTasks);
                }

                if (fitness < globalBestFitness) {
                    globalBestFitness = fitness;
                    globalBest = Arrays.copyOf(positions[i], numTasks);
                }
            }

            System.out.printf(
                    "Hybrid Iteration %d | Lambda: %.2f | Best Fitness: %.2f%n",
                    iteration + 1,
                    lambda,
                    globalBestFitness
            );
        }

        return Arrays.copyOf(globalBest, globalBest.length);
    }

    private void updateGWO(double a) {
        double[] alpha = Arrays.stream(globalBest)
                .asDoubleStream()
                .toArray();

        double[] beta = Arrays.copyOf(alpha, alpha.length);
        double[] delta = Arrays.copyOf(alpha, alpha.length);

        double alphaFitness = calculateFitness(globalBest);
        double betaFitness = Double.MAX_VALUE;
        double deltaFitness = Double.MAX_VALUE;

        for (int i = 0; i < populationSize; i++) {
            double fitness = calculateFitness(gwoPositions[i]);

            if (fitness < alphaFitness) {
                deltaFitness = betaFitness;
                delta = beta.clone();

                betaFitness = alphaFitness;
                beta = alpha.clone();

                alphaFitness = fitness;
                alpha = gwoPositions[i].clone();

            } else if (fitness < betaFitness) {
                deltaFitness = betaFitness;
                delta = beta.clone();

                betaFitness = fitness;
                beta = gwoPositions[i].clone();

            } else if (fitness < deltaFitness) {
                deltaFitness = fitness;
                delta = gwoPositions[i].clone();
            }
        }

        for (int i = 0; i < populationSize; i++) {
            double[] candidate = new double[numTasks];

            for (int j = 0; j < numTasks; j++) {
                double r1 = random.nextDouble();
                double r2 = random.nextDouble();

                double A1 = 2 * a * r1 - a;
                double C1 = 2 * r2;

                double DAlpha = Math.abs(
                        C1 * alpha[j] - gwoPositions[i][j]
                );

                double X1 = alpha[j] - A1 * DAlpha;

                r1 = random.nextDouble();
                r2 = random.nextDouble();

                double A2 = 2 * a * r1 - a;
                double C2 = 2 * r2;

                double DBeta = Math.abs(
                        C2 * beta[j] - gwoPositions[i][j]
                );

                double X2 = beta[j] - A2 * DBeta;

                r1 = random.nextDouble();
                r2 = random.nextDouble();

                double A3 = 2 * a * r1 - a;
                double C3 = 2 * r2;

                double DDelta = Math.abs(
                        C3 * delta[j] - gwoPositions[i][j]
                );

                double X3 = delta[j] - A3 * DDelta;

                candidate[j] = (X1 + X2 + X3) / 3.0;

                if (candidate[j] < 0) {
                    candidate[j] = 0;
                }

                if (candidate[j] > numVMs - 1) {
                    candidate[j] = numVMs - 1;
                }
            }

            gwoPositions[i] = candidate;
        }
    }

    private double calculateFitness(int[] solution) {
        double[] vmWorkload = new double[numVMs];

        for (int task = 0; task < numTasks; task++) {
            int vm = solution[task];
            vmWorkload[vm] += taskLengths[task];
        }

        return getMaximum(vmWorkload);
    }

    private double calculateFitness(double[] solution) {
        double[] vmWorkload = new double[numVMs];

        for (int task = 0; task < numTasks; task++) {
            int vm = (int) Math.round(solution[task]);

            if (vm < 0) {
                vm = 0;
            }

            if (vm >= numVMs) {
                vm = numVMs - 1;
            }

            vmWorkload[vm] += taskLengths[task];
        }

        return getMaximum(vmWorkload);
    }

    private double getMaximum(double[] values) {
        double maximum = 0;

        for (double value : values) {
            if (value > maximum) {
                maximum = value;
            }
        }

        return maximum;
    }

    public double[] getVmWorkloads() {
        double[] vmWorkload = new double[numVMs];

        for (int task = 0; task < numTasks; task++) {
            int vm = globalBest[task];
            vmWorkload[vm] += taskLengths[task];
        }

        return vmWorkload;
    }

    public double getBestFitness() {
        return globalBestFitness;
    }

    public List<Double> getLambdaHistory() {
        return new ArrayList<>(lambdaHistory);
    }
}
