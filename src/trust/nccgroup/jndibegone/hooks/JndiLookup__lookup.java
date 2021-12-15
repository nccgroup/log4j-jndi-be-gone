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

package trust.nccgroup.jndibegone.hooks;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import trust.nccgroup.jndibegone.logger.Logger;

import java.lang.instrument.Instrumentation;

public class JndiLookup__lookup {

  private static final String TAG = "JndiLookup__lookup";

  public volatile static Logger LOGGER = null;

  private final ElementMatcher<TypeDescription> typeMatcher;

  public JndiLookup__lookup(ElementMatcher<TypeDescription> typeMatcher, Logger logger) {
    this.typeMatcher = typeMatcher;
    LOGGER = logger;
  }

  @Advice.OnMethodEnter(inline = true, skipOn = Advice.OnNonDefaultValue.class)
  static String enter(@Advice.Argument(readOnly = true, value = 1) String key) {
    if (LOGGER != null) {
      final String sanitizedMsg = "log4j jndi lookup attempted: (sanitized) " + key.replace("$", "%").replace("{", "_").replace("}", "_");
      LOGGER.log(sanitizedMsg);
    }
    return "(log4j jndi disabled)";
  }

  @Advice.OnMethodExit(inline = true, onThrowable = Exception.class)
  static void exit(@Advice.Enter String enter_return, @Advice.Return(readOnly = false) String ret) {
    ret = enter_return;
  }

  static AsmVisitorWrapper getVisitor() {
    return Advice.to(JndiLookup__lookup.class).on(ElementMatchers.named("lookup"));
  }

  public void hook(Instrumentation inst) {
    new AgentBuilder.Default()
    .disableClassFormatChanges()
    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
    .ignore(ElementMatchers.none())
    .type(typeMatcher)
    .transform(new AgentBuilder.Transformer() {
      public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription td, ClassLoader cl, JavaModule mod) {
        return builder.visit(JndiLookup__lookup.getVisitor());
      }
    })
    .installOn(inst);
  }

}
