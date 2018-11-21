/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.workbench.screens.scenariosimulation.backend.server;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.drools.workbench.screens.scenariosimulation.backend.server.runner.AbstractScenarioRunner;
import org.drools.workbench.screens.scenariosimulation.backend.server.runner.ScenarioRunnerImpl;
import org.drools.workbench.screens.scenariosimulation.model.ScenarioSimulationModel;
import org.drools.workbench.screens.scenariosimulation.model.Simulation;
import org.drools.workbench.screens.scenariosimulation.service.ScenarioRunnerService;
import org.guvnor.common.services.shared.test.Failure;
import org.guvnor.common.services.shared.test.TestResultMessage;
import org.jboss.errai.bus.server.annotations.Service;
import org.junit.runner.Result;
import org.kie.api.runtime.KieContainer;
import org.kie.workbench.common.services.backend.builder.ModuleBuildInfo;
import org.kie.workbench.common.services.shared.project.KieModule;
import org.kie.workbench.common.services.shared.project.KieModuleService;
import org.uberfire.backend.vfs.Path;

import static org.drools.workbench.screens.scenariosimulation.backend.server.util.JunitRunnerHelper.runWithJunit;

@Service
@ApplicationScoped
public class ScenarioRunnerServiceImpl
        implements ScenarioRunnerService {

    private Event<TestResultMessage> defaultTestResultMessageEvent;

    private KieModuleService moduleService;

    private ModuleBuildInfo moduleBuildInfo;

    private BiFunction<KieContainer, Simulation, AbstractScenarioRunner> runnerSupplier = ScenarioRunnerImpl::new;

    public ScenarioRunnerServiceImpl() {
        //CDI Proxy
    }

    @Inject
    public ScenarioRunnerServiceImpl(final Event<TestResultMessage> defaultTestResultMessageEvent,
                                     final KieModuleService moduleService,
                                     final ModuleBuildInfo moduleBuildInfo) {
        this.defaultTestResultMessageEvent = defaultTestResultMessageEvent;
        this.moduleService = moduleService;
        this.moduleBuildInfo = moduleBuildInfo;
    }

    @Override
    public void runAllTests(final String identifier,
                            final Path path) {

        defaultTestResultMessageEvent.fire(
                new TestResultMessage(
                        identifier,
                        1,
                        1,
                        new ArrayList<>()));
    }

    @Override
    public void runAllTests(final String identifier,
                            final Path path,
                            final Event<TestResultMessage> customTestResultEvent) {

        customTestResultEvent.fire(
                new TestResultMessage(
                        identifier,
                        1,
                        1,
                        new ArrayList<>()));
    }

    @Override
    public void runTest(final String identifier,
                        final Path path,
                        final ScenarioSimulationModel model) {

        KieModule kieModule = getKieModule(path);
        ClassLoader moduleClassLoader = moduleBuildInfo.getOrCreateEntry(kieModule).getClassLoader();
        KieContainer kieContainer = getKieContainer(kieModule);
        AbstractScenarioRunner scenarioRunner = getRunnerSupplier().apply(kieContainer, model.getSimulation());

        scenarioRunner.setClassLoader(moduleClassLoader);

        final List<Failure> failures = new ArrayList<>();

        final List<Failure> failureDetails = new ArrayList<>();

        Result result = runWithJunit(scenarioRunner, failures, failureDetails);

        defaultTestResultMessageEvent.fire(
                new TestResultMessage(
                        identifier,
                        result.getRunCount(),
                        result.getRunTime(),
                        failures));
    }

    protected KieModule getKieModule(Path path) {
        return moduleService.resolveModule(path);
    }

    protected KieContainer getKieContainer(KieModule kieModule) {
        return moduleBuildInfo.getOrCreateEntry(kieModule).getKieContainer().orElse(null);
    }

    public BiFunction<KieContainer, Simulation, AbstractScenarioRunner> getRunnerSupplier() {
        return runnerSupplier;
    }

    public void setRunnerSupplier(BiFunction<KieContainer, Simulation, AbstractScenarioRunner> runnerSupplier) {
        this.runnerSupplier = runnerSupplier;
    }
}
