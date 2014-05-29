package core;

import com.google.inject.Inject;
import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.po.Artifact;
import org.jenkinsci.test.acceptance.po.ArtifactArchiver;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;
import org.jenkinsci.test.acceptance.po.Slave;
import org.jenkinsci.test.acceptance.slave.SlaveController;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Feature: Archive artifacts
 */
public class ArtifactsTest extends AbstractJUnitTest {

    public static final int LARGE_FILE_GB = 3;
    public static final int NO_SMALL_FILES = 200;

    @Inject
    private SlaveController slaveController;
    private FreeStyleJob job;
    private Slave slave;

    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        job = jenkins.jobs.create();
        slave = slaveController.install(jenkins).get();
    }

    /**
     * Tests archiving a large file.
     * {@link #LARGE_FILE_GB} GB in size.
     * Assumes some type of linux slave with available disk space.
     */
    @Test
    public void test_large_file() {
        job.configure();
        job.setLabelExpression(slave.getName());
        job.addShellStep("#!/bin/bash\n" +
                "dd if=/dev/zero of="+LARGE_FILE_GB+"GB-${BUILD_NUMBER}-file.txt bs="+LARGE_FILE_GB+"M count=1000\n" +
                "ls -l");
        ArtifactArchiver archiver = job.addPublisher(ArtifactArchiver.class);
        archiver.includes("*-file.txt");
        job.save();
        Build build = job.scheduleBuild().waitUntilFinished(240);
        Artifact artifact = build.getArtifact(LARGE_FILE_GB + "GB-"+build.getNumber()+"-file.txt");
        assertNotNull(artifact);
        artifact.assertThatExists(true);
    }

    /**
     * Tests archiving a number of small files.
     * Creates and archives {@link #NO_SMALL_FILES} files each 1KB in size.
     * Assumes some type of linux slave with available disk space.
     */
    @Test
    public void test_many_small_files() {
        job.configure();
        job.setLabelExpression(slave.getName());
        job.addShellStep("#!/bin/bash\n" +
                "rm ./job*.txt\n" +
                "for i in {1.."+NO_SMALL_FILES+"}\n" +
                "do\n" +
                " dd if=/dev/zero of=job-${BUILD_NUMBER}-file-$i.txt bs=1k count=1\n" +
                "done\n" +
                "ls -l");
        ArtifactArchiver archiver = job.addPublisher(ArtifactArchiver.class);
        archiver.includes("*-file*.txt");
        job.save();
        Build build = job.scheduleBuild().waitUntilFinished();
        List<Artifact> artifacts = build.getArtifacts();
        assertNotNull(artifacts);
        assertEquals(NO_SMALL_FILES, artifacts.size());
    }
}