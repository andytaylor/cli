/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon;

import org.jboss.pnc.bacon.cli.Da;
import org.jboss.pnc.bacon.cli.Pig;
import org.jboss.pnc.bacon.cli.Pnc;
import picocli.CommandLine;

import java.util.List;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 12/13/18
 */
@CommandLine.Command(name = "java -jar bacon.jar",
        mixinStandardHelpOptions = true,
        subcommands = {Pig.class, Pnc.class, Da.class}
)
public class App {

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(App.class);
        List<CommandLine> parsed = commandLine.parse(args);
        CommandLine command = deepestCommand(parsed);

        new CommandLine.RunLast().handleParseResult(command.getParseResult());
    }

    private static CommandLine deepestCommand(List<CommandLine> parsed) {
        return parsed.get(parsed.size() - 1);
    }
}