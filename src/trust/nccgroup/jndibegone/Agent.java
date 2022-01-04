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

import java.lang.instrument.Instrumentation;
import trust.nccgroup.jndibegone.hooks.JndiLookup__lookup;

import java.util.HashMap;

@SuppressWarnings("unused")
public class Agent {

  public static void load(String _args, Instrumentation inst) {
    boolean deep_match = true;

    try {
      HashMap<String,String> args = new HashMap<String,String>();
      if (_args != null && !"".equals(_args)) {
        String[] pairs = _args.split("&");
        for (String pair : pairs) {
          int epos = pair.indexOf("=");
          if (epos == -1) {
            System.err.println("[log4j-jndi-be-gone] ignoring invalid agent argument: " + pair);
            continue;
          }
          String k = pair.substring(0, epos);
          String v = pair.substring(epos+1, pair.length());
          if (v.indexOf("=") != -1) {
            System.err.println("[log4j-jndi-be-gone] ignoring invalid agent argument: " + pair);
            continue;
          }
          args.put(k, v);
        }

        String structureMatch = null;
        if (args.containsKey("structureMatch")) {
          structureMatch = args.get("structureMatch");
        } else {
          structureMatch = "1";
        }
        if ("0".equals(structureMatch)) {
          deep_match = false;
        } else if (!"1".equals(structureMatch)) {
          System.err.println("[log4j-jndi-be-gone] ignoring invalid structureMatch argument: " + structureMatch);
        }
      }

      if (deep_match) {
        JndiLookup__lookup.hook_deep_match(inst);
      } else {
        JndiLookup__lookup.hook(inst);
      }
    } catch (Throwable t) {
      System.err.println("[log4j-jndi-be-gone] Exception raised in agent.");
      t.printStackTrace();
    }
  }
}
