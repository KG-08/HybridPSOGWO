package org.kandarp;

import org.cloudsimplus.cloudlets.Cloudlet;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class PSOScheduler {

    private final int numTasks;
    private final int numVMs;
    private final int populationSize;
    private final int iterations;

    private final double[] taskLengths;

    private int[][] positions;
    private double[][] velocities;

    private int[][] personalBest;
    private double[] personalBestFitness;

    private int[] globalBest;

    private double globalBestFitness =
            Double.MAX_VALUE;

    private final Random random =
            new Random(123);

    public PSOScheduler(
            List<Cloudlet> cloudlets,
            int numVMs,
            int populationSize,
            int iterations) {

        this.numTasks =
                cloudlets.size();

        this.numVMs =
                numVMs;

        this.populationSize =
                populationSize;

        this.iterations =
                iterations;

        this.taskLengths =
                new double[numTasks];

        for (int i = 0;
             i < numTasks;
             i++) {

            taskLengths[i] =
                    cloudlets.get(i).getLength();
        }

        positions =
                new int[populationSize][numTasks];

        velocities =
                new double[populationSize][numTasks];

        personalBest =
                new int[populationSize][numTasks];

        personalBestFitness =
                new double[populationSize];

        globalBest =
                new int[numTasks];

        initialize();
    }

    // ---------------- INITIALIZE ----------------

    private void initialize() {

        for (int i = 0;
             i < populationSize;
             i++) {

            for (int j = 0;
                 j < numTasks;
                 j++) {

                positions[i][j] =
                        random.nextInt(numVMs);

                velocities[i][j] =
                        random.nextDouble() * 2 - 1;
            }

            personalBest[i] =
                    Arrays.copyOf(
                            positions[i],
                            numTasks
                    );

            personalBestFitness[i] =
                    calculateFitness(
                            positions[i]
                    );

            if (personalBestFitness[i]
                    < globalBestFitness) {

                globalBestFitness =
                        personalBestFitness[i];

                globalBest =
                        Arrays.copyOf(
                                positions[i],
                                numTasks
                        );
            }
        }
    }

    // ---------------- PSO OPTIMIZATION ----------------

    public int[] optimize() {

        double inertia = 0.7;
        double c1 = 1.5;
        double c2 = 1.5;

        for (int iteration = 0;
             iteration < iterations;
             iteration++) {

            for (int i = 0;
                 i < populationSize;
                 i++) {

                for (int j = 0;
                     j < numTasks;
                     j++) {

                    double r1 =
                            random.nextDouble();

                    double r2 =
                            random.nextDouble();

                    velocities[i][j] =
                            inertia *
                                    velocities[i][j]

                                    + c1 * r1 *
                                    (personalBest[i][j]
                                            - positions[i][j])

                                    + c2 * r2 *
                                    (globalBest[j]
                                            - positions[i][j]);

                    double newPosition =
                            positions[i][j]
                                    + velocities[i][j];

                    int vm =
                            (int) Math.round(
                                    newPosition
                            );

                    if (vm < 0) {
                        vm = 0;
                    }

                    if (vm >= numVMs) {
                        vm = numVMs - 1;
                    }

                    positions[i][j] =
                            vm;
                }

                double fitness =
                        calculateFitness(
                                positions[i]
                        );

                if (fitness
                        < personalBestFitness[i]) {

                    personalBestFitness[i] =
                            fitness;

                    personalBest[i] =
                            Arrays.copyOf(
                                    positions[i],
                                    numTasks
                            );
                }

                if (fitness
                        < globalBestFitness) {

                    globalBestFitness =
                            fitness;

                    globalBest =
                            Arrays.copyOf(
                                    positions[i],
                                    numTasks
                            );
                }
            }

            System.out.printf(
                    "PSO Iteration %d | Best Fitness: %.2f%n",
                    iteration + 1,
                    globalBestFitness
            );
        }

        return globalBest;
    }

    // ---------------- FITNESS FUNCTION ----------------

    private double calculateFitness(
            int[] solution) {

        double[] vmWorkload =
                new double[numVMs];

        for (int task = 0;
             task < numTasks;
             task++) {

            int vm =
                    solution[task];

            double taskLength =
                    taskLengths[task];

            vmWorkload[vm] +=
                    taskLength;
        }

        double makespan = 0;

        for (double workload :
                vmWorkload) {

            if (workload > makespan) {

                makespan =
                        workload;
            }
        }

        return makespan;
    }

    // ---------------- VM WORKLOADS ----------------

    public double[] getVmWorkloads() {

        double[] vmWorkload =
                new double[numVMs];

        for (int task = 0;
             task < numTasks;
             task++) {

            int vm =
                    globalBest[task];

            vmWorkload[vm] +=
                    taskLengths[task];
        }

        return vmWorkload;
    }

    // ---------------- GET BEST FITNESS ----------------

    public double getBestFitness() {

        return globalBestFitness;
    }
}