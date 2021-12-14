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

import trust.nccgroup.jndibegone.hooks.JndiLookup__lookup;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class Agent {

  public static final String OPTION_LOG_DIR = "logDir";

  public static void load(String args, Instrumentation inst) {
    final Map<String, String> options = new HashMap<String, String>();
    for (String arg : args.split(",")) {
      final int i = arg.indexOf("=");
      final String key = i < 0 ? arg : arg.substring(0, i).trim();
      final String value = i < 0 ? null : arg.substring(i + 1).trim();
      options.put(key, value);
    }

    final String logDirPath = options.get(OPTION_LOG_DIR);
    final Logger logger = new Logger(logDirPath);
    final JndiLookupClassFinder classFinder = new JndiLookupClassFinder(logger);
    final Set<String> lookupClassNames = classFinder.findJndiLookupClassNames();

    new JndiLookup__lookup(lookupClassNames, logger).hook(inst);
  }

}
