/*
Copyright 2021 NCC Group

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package trust.nccgroup.jndibegone;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import trust.nccgroup.jndibegone.config.Config;
import trust.nccgroup.jndibegone.hooks.JndiLookup__lookup;
import trust.nccgroup.jndibegone.logger.Logger;

import java.lang.instrument.Instrumentation;

@SuppressWarnings("unused")
public class Agent {

  public static void load(String args, Instrumentation inst) {
    final Config config = Config.parse(args);
    final Logger logger = new Logger(config.logDir, config.logToStdErr);
    final ElementMatcher<TypeDescription> typeMatcher = new JndiLookupClassMatcher(
      config.excludeClassPattern,
      config.includeClassPattern,
      config.classSigMode,
      logger
    );

    new JndiLookup__lookup(typeMatcher, logger).hook(inst);
  }

}
