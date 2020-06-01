package com.redhat.demos.main;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kie.api.KieServices;
import org.kie.api.command.Command;
import org.kie.api.command.KieCommands;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieServiceResponse.ResponseType;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.RuleServicesClient;

import com.redhat.demos.drools_demo_project.Measurement;

public class MyConfigurationObject {

	private static final String URL = "http://34.87.47.141:8080/kie-server/services/rest/server";
    private static final String USER = "kieadm";
    private static final String PASSWORD = "kieadm";

    private static final MarshallingFormat FORMAT = MarshallingFormat.JSON;

    private static KieServicesConfiguration conf;
    private static KieServicesClient kieServicesClient;
    
    private static KieServices kieServices;
	  
    public static void initialize() {
        conf = KieServicesFactory.newRestConfiguration(URL, USER, PASSWORD);
        conf.setMarshallingFormat(FORMAT);
        Set<Class<?>> allClasses = new HashSet<Class<?>>();
        allClasses.add(Measurement.class);
        conf.addExtraClasses(allClasses);
        kieServicesClient = KieServicesFactory.newKieServicesClient(conf);
        kieServices = KieServices.Factory.get();
    }
    
    public static void disposeAndCreateContainer() {
        System.out.println("== Disposing and creating containers ==");

        // Retrieve list of KIE containers
        List<KieContainerResource> kieContainers = kieServicesClient.listContainers().getResult().getContainers();
        if (kieContainers.size() == 0) {
            System.out.println("No containers available...");
            return;
        }

        // Dispose KIE container
        KieContainerResource container = kieContainers.get(0);
        String containerId = container.getContainerId();
        ServiceResponse<Void> responseDispose = kieServicesClient.disposeContainer(containerId);
        if (responseDispose.getType() == ResponseType.FAILURE) {
            System.out.println("Error disposing " + containerId + ". Message: ");
            System.out.println(responseDispose.getMsg());
            return;
        }
        System.out.println("Success Disposing container " + containerId);
        
        System.out.println("Executing Rules .... ");
        RuleServicesClient rulesClient = kieServicesClient.getServicesClient(RuleServicesClient.class);
        KieCommands commandsFactory = kieServices.getCommands();
        
        Measurement m = new Measurement("test", "test");
        
        Command<?> insert = commandsFactory.newInsert(m);
        Command<?> fireAllRules = commandsFactory.newFireAllRules();
        Command<?> batchCommand = commandsFactory.newBatchExecution(Arrays.asList(insert, fireAllRules));
        
        ServiceResponse<String> executeResponse = rulesClient.executeCommands(containerId, batchCommand);
        
        if(executeResponse.getType() == ResponseType.SUCCESS) {
            System.out.println("Commands executed with success! Response: ");
            System.out.println(executeResponse.getResult());
          } else {
            System.out.println("Error executing rules. Message: ");
            System.out.println(executeResponse.getMsg());
          }
        
        }
    
	public static void main(String[] args) {
		initialize();
		disposeAndCreateContainer();

	}

}
