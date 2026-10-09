
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class RunController {

    @PostMapping("/api/run")
    public Map<String, Object> runSimulation(
            @RequestParam(defaultValue = "20") int taskCount) {

        if (taskCount < 1 || taskCount > 1000) {
            throw new IllegalArgumentException(
                    "Task count must be between 1 and 1000"
            );
        }

        CloudSimPlus simulation = new CloudSimPlus();

        List<Host> hosts = createHosts();
        List<Vm> vms = createVMs();

        new DatacenterSimple(simulation, hosts);

        DatacenterBroker broker =
                new DatacenterBrokerSimple(simulation);

        LiveTaskGenerator generator = new LiveTaskGenerator();

        List<Cloudlet> tasks = generator.generateTasks(taskCount);

        HybridPSOGWOScheduler scheduler =
                new HybridPSOGWOScheduler(
                        tasks,
                        vms.size(),
                        20,
                        20
                );

        int[] mapping = scheduler.optimize();

        broker.submitVmList(vms);

        for (int i = 0; i < tasks.size(); i++) {
            broker.bindCloudletToVm(
                    tasks.get(i),
                    vms.get(mapping[i])
            );
        }

        broker.submitCloudletList(tasks);

        simulation.start();

        List<Cloudlet> finished =
                broker.getCloudletFinishedList();

        double makespan = finished.stream()
                .mapToDouble(Cloudlet::getFinishTime)
                .max()
                .orElse(0);

        double throughput = makespan > 0
                ? finished.size() / makespan
                : 0;

        double[] workloads = scheduler.getVmWorkloads();

        double minimum = Double.MAX_VALUE;
        double maximum = Double.MIN_VALUE;

        for (double workload : workloads) {
            minimum = Math.min(minimum, workload);
            maximum = Math.max(maximum, workload);
        }

        double loadBalance = maximum > 0
                ? (minimum / maximum) * 100
                : 100;

        List<Map<String, Object>> allocations = new ArrayList<>();

        for (int i = 0; i < tasks.size(); i++) {
            Map<String, Object> allocation = new HashMap<>();

            allocation.put("task", tasks.get(i).getId());
            allocation.put("length", tasks.get(i).getLength());
            allocation.put("vm", mapping[i]);

            allocations.add(allocation);
        }

        List<Map<String, Object>> vmData = new ArrayList<>();

        for (int i = 0; i < workloads.length; i++) {
            Map<String, Object> vm = new HashMap<>();

            vm.put("vm", i);
            vm.put("workload", workloads[i]);

            vmData.add(vm);
        }

        Map<String, Object> result = new HashMap<>();

        result.put("scheduler", "Hybrid PSO-GWO");
        result.put("simulation", "CloudSim Plus");
        result.put("tasksSubmitted", tasks.size());
        result.put("tasksCompleted", finished.size());
        result.put("makespan", makespan);
        result.put("throughput", throughput);
        result.put("bestFitness", scheduler.getBestFitness());
        result.put("loadBalance", loadBalance);
        result.put("allocations", allocations);
        result.put("vmWorkloads", vmData);

        // Actual lambda values used during the optimization iterations.
        result.put("lambdaHistory", scheduler.getLambdaHistory());

        return result;
    }

    private List<Host> createHosts() {
        List<Host> hosts = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            List<Pe> peList = new ArrayList<>();
            peList.add(new PeSimple(1000));

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
        List<Vm> vms = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            Vm vm = new VmSimple(1000, 1);

            vm.setRam(2048)
                    .setBw(1000)
                    .setSize(10000);

            vms.add(vm);
        }

        return vms;
    }
}

