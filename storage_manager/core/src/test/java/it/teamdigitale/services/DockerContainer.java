package it.teamdigitale.services;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.WaitContainerResultCallback;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Copied from https://github.com/eurodyn/Qlack2/blob/f708d8652db891e63aafac41b987267b012bfeef/Util/qlack2-util-docker/src/main/java/com/eurodyn/qlack2/util/docker/DockerContainer.java
 */
public class DockerContainer {

    /**
     * JUL reference
     */
    private final static Logger LOGGER = Logger.getLogger(DockerContainer.class.getName());

    private String imageName;
    private Map<String, String> ports = new HashMap<>();
    private Map<String, String> env = new HashMap<>();
    private String name;
    private boolean forcePull = false;
    private boolean outputToConsole = false;
    private String dockerEngine = "unix:///var/run/docker.sock";
    private String id;

    public static DockerContainer builder() {
        return new DockerContainer();
    }

    public DockerContainer withId(String id) {
        this.id = id;
        return this;
    }

    public DockerContainer withDockerEngine(String uri) {
        this.dockerEngine = uri;
        return this;
    }

    public DockerContainer withImage(String imageName) {
        this.imageName = imageName;
        return this;
    }

    public DockerContainer withPort(String exposedPort, String containerPort) {
        ports.put(exposedPort, containerPort);
        return this;
    }

    public DockerContainer withName(String name) {
        this.name = name;
        return this;
    }

    public DockerContainer withEnv(String key, String val) {
        env.put(key, val);
        return this;
    }

    public DockerContainer outputToConsole() {
        outputToConsole = true;
        return this;
    }

    public DockerContainer forcePull() {
        forcePull = true;
        return this;
    }

    private void debug(String msg, Object...arguments) {
        if (outputToConsole) {
            System.out.println(MessageFormat.format(msg, arguments));
        }
        LOGGER.log(Level.INFO, msg, arguments);
    }

    private DockerClient createDockerClient() {
        /** Configure the client */
        debug("Creating Docker client for {0}...", dockerEngine);
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerEngine)
                .build();
        final DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();
        debug("Docker client for {0} created.", dockerEngine);

        return dockerClient;
    }

    /**
     * Runs the configured container and returns the ID of the created container
     * @return Returns the ID of the container that was created.
     */
    public String run() {
        DockerClient dockerClient = createDockerClient();

        /** Pull the image if required */
        if (forcePull
                || dockerClient.listImagesCmd().withImageNameFilter(imageName).exec().size() == 0) {
            debug("Pulling image {0}...", imageName);
            dockerClient.pullImageCmd(imageName).exec(new PullImageResultCallback()).awaitSuccess();
            debug("Image {0} pulled.", imageName);
        }

        /** Create port bindings and container */
        debug("Creating container for image {0}...", imageName);
        CreateContainerResponse createContainerResponse = dockerClient.createContainerCmd(imageName)
                .withExposedPorts(ports.entrySet().stream().map(p -> ExposedPort.parse(p.getKey()))
                        .collect(Collectors.toList()))
                .withPortBindings(
                        ports.entrySet().stream().map(p -> PortBinding.parse(p.getKey() + ":" + p.getValue()))
                                .collect(Collectors.toList()))
                .withName(name)
                .withEnv(env.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.toList()))
                .exec();
        String containerId = createContainerResponse.getId();
        debug("Container for image {0} created with id {1}.", new Object[]{imageName, containerId});


        /** Run container */
        debug("Running container {0}...", containerId);
        dockerClient.startContainerCmd(containerId).exec();
        debug("Container {0} ran.", containerId);

        id = createContainerResponse.getId();

        return id;
    }

    private void stop(DockerClient dockerClient, String id) {
        debug("Sending command to stop container {0}...", id);
        dockerClient.stopContainerCmd(id).exec();
        debug("Command to stop container {0} sent.", id);
    }

    public void stop() {
        DockerClient dockerClient = createDockerClient();
        stop(dockerClient, id);
    }

    private void stopAndWait(DockerClient dockerClient, String id) {
        stop(dockerClient, id);
        try {
            debug("Waiting for container {0} to stop...", id);
            dockerClient.waitContainerCmd(id)
                    .exec(new WaitContainerResultCallback()).awaitCompletion();
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        debug("Container {0} stopped.", id);
    }

    public void stopAndWait() {
        DockerClient dockerClient = createDockerClient();
        stopAndWait(dockerClient, id);
    }

    private void remove(DockerClient dockerClient, String id) {
        dockerClient.removeContainerCmd(id).exec();
    }

    public void remove() {
        DockerClient dockerClient = createDockerClient();
        dockerClient.removeContainerCmd(id).exec();
    }

    public void clean() {
        DockerClient dockerClient = createDockerClient();
        stopAndWait(dockerClient, id);
        remove(dockerClient, id);
    }

}