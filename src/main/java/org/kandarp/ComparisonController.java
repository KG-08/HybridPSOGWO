package org.kandarp;

import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
public class ComparisonController {

    @PostMapping("/api/compare")
    public Map<String, Object> compareSchedulers() {

        int taskCount = 100;
        int populationSize = 20;
        int iterations = 50;
        int numberOfVMs = 4;

        List<Cloudlet> baseTasks =
                generateResearchTasks(taskCount);

        List<Map<String, Object>> results =
                new ArrayList<>();

        results.add(
                runBaseline(
                        baseTasks,
                        numberOfVMs
                )
        );

        results.add(
                runPSO(
                        baseTasks,
                        numberOfVMs,
                        populationSize,
                        iterations
                )
        );

        results.add(
                runGWO(
                        baseTasks,
                        numberOfVMs,
                        populationSize,
                        iterations
                )
        );

        results.add(
                runHybrid(
                        baseTasks,
                        numberOfVMs,
                        populationSize,
                        iterations
                )
        );

        Map<String, Object> response =
                new HashMap<>();

        response.put("tasks", taskCount);
        response.put("vms", numberOfVMs);
        response.put("population", populationSize);
        response.put("iterations", iterations);
        response.put("results", results);

        return response;
    }

    private List<Cloudlet> generateResearchTasks(
            int numberOfTasks) {

        List<Cloudlet> tasks =
                new ArrayList<>();

        Random random =
                new Random(42);

        for (int i = 0; i < numberOfTasks; i++) {

            int length =
                    100 + random.nextInt(901);

            Cloudlet cloudlet =
                    new org.cloudsimplus.cloudlets.CloudletSimple(
                            length,
                            1
                    );

            cloudlet.setSizes(1024);

            tasks.add(cloudlet);
        }

        return tasks;
    }

    private Map<String, Object> runBaseline(
            List<Cloudlet> baseTasks,
            int numberOfVMs) {

        List<Cloudlet> tasks =
                copyTasks(baseTasks);

        CloudSimPlus simulation =
                new CloudSimPlus();

        List<Host> hosts =
                createHosts();

        List<Vm> vms =
                createVMs();

        new DatacenterSimple(
                simulation,
                hosts
        );

        DatacenterBroker broker =
                new DatacenterBrokerSimple(
                        simulation
                );

        broker.submitVmList(vms);

        double[] workloads =
                new double[numberOfVMs];

        for (Cloudlet task : tasks) {

            int selectedVm = 0;

            for (int i = 1; i < numberOfVMs; i++) {

                if (workloads[i] <
                        workloads[selectedVm]) {

                    selectedVm = i;
                }
            }

            workloads[selectedVm] +=
                    task.getLength();

            broker.bindCloudletToVm(
                    task,
                    vms.get(selectedVm)
            );
        }

        broker.submitCloudletList(tasks);

        simulation.start();

        return createResult(
                "Baseline",
                broker,
                workloads,
                null
        );
    }

    private Map<String, Object> runPSO(
            List<Cloudlet> baseTasks,
            int numberOfVMs,
            int populationSize,
            int iterations) {

        List<Cloudlet> tasks =
                copyTasks(baseTasks);

        CloudSimPlus simulation =
                new CloudSimPlus();

        List<Host> hosts =
                createHosts();

        List<Vm> vms =
                createVMs();

        new DatacenterSimple(
                simulation,
                hosts
        );

        DatacenterBroker broker =
                new DatacenterBrokerSimple(
                        simulation
                );

        PSOScheduler scheduler =
                new PSOScheduler(
                        tasks,
                        numberOfVMs,
                        populationSize,
                        iterations
                );

        int[] mapping =
                scheduler.optimize();

        broker.submitVmList(vms);

        for (int i = 0; i < tasks.size(); i++) {

            broker.bindCloudletToVm(
                    tasks.get(i),
                    vms.get(mapping[i])
            );
        }

        broker.submitCloudletList(tasks);

        simulation.start();

        return createResult(
                "PSO",
                broker,
                scheduler.getVmWorkloads(),
                scheduler.getBestFitness()
        );
    }

    private Map<String, Object> runGWO(
            List<Cloudlet> baseTasks,
            int numberOfVMs,
            int populationSize,
            int iterations) {

        List<Cloudlet> tasks =
                copyTasks(baseTasks);

        CloudSimPlus simulation =
                new CloudSimPlus();

        List<Host> hosts =
                createHosts();

        List<Vm> vms =
                createVMs();

        new DatacenterSimple(
                simulation,
                hosts
        );

        DatacenterBroker broker =
                new DatacenterBrokerSimple(
                        simulation
                );

        GWOScheduler scheduler =
                new GWOScheduler(
                        tasks,
                        numberOfVMs,
                        populationSize,
                        iterations
                );

        int[] mapping =
                scheduler.optimize();

        broker.submitVmList(vms);

        for (int i = 0; i < tasks.size(); i++) {

            broker.bindCloudletToVm(
                    tasks.get(i),
                    vms.get(mapping[i])
            );
        }

        broker.submitCloudletList(tasks);

        simulation.start();

        return createResult(
                "GWO",
                broker,
                scheduler.getVmWorkloads(),
                scheduler.getBestFitness()
        );
    }

    private Map<String, Object> runHybrid(
            List<Cloudlet> baseTasks,
            int numberOfVMs,
            int populationSize,
            int iterations) {

        List<Cloudlet> tasks =
                copyTasks(baseTasks);

        CloudSimPlus simulation =
                new CloudSimPlus();

        List<Host> hosts =
                createHosts();

        List<Vm> vms =
                createVMs();

        new DatacenterSimple(
                simulation,
                hosts
        );

        DatacenterBroker broker =
                new DatacenterBrokerSimple(
                        simulation
                );

        HybridPSOGWOScheduler scheduler =
                new HybridPSOGWOScheduler(
                        tasks,
                        numberOfVMs,
                        populationSize,
                        iterations
                );

        int[] mapping =
                scheduler.optimize();

        broker.submitVmList(vms);

        for (int i = 0; i < tasks.size(); i++) {

            broker.bindCloudletToVm(
                    tasks.get(i),
                    vms.get(mapping[i])
            );
        }

        broker.submitCloudletList(tasks);

        simulation.start();

        return createResult(
                "Hybrid PSO-GWO",
                broker,
                scheduler.getVmWorkloads(),
                scheduler.getBestFitness()
        );
    }

    private Map<String, Object> createResult(
            String schedulerName,
            DatacenterBroker broker,
            double[] workloads,
            Double bestFitness) {

        List<Cloudlet> finished =
                broker.getCloudletFinishedList();

        double makespan =
                finished.stream()
                        .mapToDouble(
                                Cloudlet::getFinishTime
                        )
                        .max()
                        .orElse(0);

        double throughput =
                makespan > 0
                        ? finished.size() / makespan
                        : 0;

        double minimum =
                Double.MAX_VALUE;

        double maximum =
                Double.MIN_VALUE;

        for (double workload : workloads) {

            minimum =
                    Math.min(
                            minimum,
                            workload
                    );

            maximum =
                    Math.max(
                            maximum,
                            workload
                    );
        }

        double loadBalance =
                maximum > 0
                        ? (minimum / maximum) * 100
                        : 100;

        Map<String, Object> result =
                new HashMap<>();

        result.put(
                "scheduler",
                schedulerName
        );

        result.put(
                "tasksCompleted",
                finished.size()
        );

        result.put(
                "makespan",
                makespan
        );

        result.put(
                "throughput",
                throughput
        );

        result.put(
                "loadBalance",
                loadBalance
        );

        if (bestFitness != null) {

            result.put(
                    "bestFitness",
                    bestFitness
            );
        }

        List<Map<String, Object>> vmData =
                new ArrayList<>();

        for (int i = 0;
             i < workloads.length;
             i++) {

            Map<String, Object> vm =
                    new HashMap<>();

            vm.put(
                    "vm",
                    i
            );

            vm.put(
                    "workload",
                    workloads[i]
            );

            vmData.add(vm);
        }

        result.put(
                "vmWorkloads",
                vmData
        );

        return result;
    }

    private List<Cloudlet> copyTasks(
            List<Cloudlet> original) {

        List<Cloudlet> copy =
                new ArrayList<>();

        for (Cloudlet task : original) {

            Cloudlet newTask =
                    new org.cloudsimplus.cloudlets.CloudletSimple(
                            task.getLength(),
                            1
                    );

            newTask.setSizes(1024);

            copy.add(newTask);
        }

        return copy;
    }

    private List<Host> createHosts() {

        List<Host> hosts =
                new ArrayList<>();

        for (int i = 0; i < 4; i++) {

            List<Pe> peList =
                    new ArrayList<>();

            peList.add(
                    new PeSimple(1000)
            );

            hosts.add(
                    new HostSimple(
                            4096,
                            10000,
                            1000000,
                            peList
                    )
            );
        }

        return hosts;
    }

    private List<Vm> createVMs() {

        List<Vm> vms =
                new ArrayList<>();

        for (int i = 0; i < 4; i++) {

            Vm vm =
                    new VmSimple(
                            1000,
                            1
                    );

            vm.setRam(2048)
                    .setBw(1000)
                    .setSize(10000);

            vms.add(vm);
        }

        return vms;
    }
}