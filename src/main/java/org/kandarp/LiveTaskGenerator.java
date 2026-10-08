package org.kandarp;

import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LiveTaskGenerator {

    private final Random random;

    public LiveTaskGenerator() {
        random = new Random(100);
    }

    public Cloudlet generateTask() {

        int length =
                100 + random.nextInt(1901);

        Cloudlet cloudlet =
                new CloudletSimple(
                        length,
                        1
                );

        cloudlet.setSizes(1024);

        return cloudlet;
    }

    public List<Cloudlet> generateTasks(
            int numberOfTasks) {

        List<Cloudlet> tasks =
                new ArrayList<>();

        for (int i = 0;
             i < numberOfTasks;
             i++) {

            tasks.add(
                    generateTask()
            );
        }

        return tasks;
    }
}