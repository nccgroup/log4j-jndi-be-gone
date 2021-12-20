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

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationDescription;
import java.lang.annotation.ElementType;
import java.util.Set;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.field.FieldDescription;
import java.util.HashMap;
import net.bytebuddy.description.type.TypeList;
import java.util.Arrays;
import java.lang.reflect.Modifier;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.method.ParameterDescription;

import java.lang.instrument.Instrumentation;

public class JndiLookup__lookup {

  private static final String TAG = "JndiLookup__lookup";

  @Advice.OnMethodEnter(inline = true, skipOn = Advice.OnNonDefaultValue.class)
  static String enter(@Advice.This Object _this, @Advice.Argument(readOnly = true, value = 1) String key) {
    System.err.println("[log4j-jndi-be-gone] jndi lookup attempted in " + _this.getClass() + ": (sanitized) " + key.replace("$", "%").replace("{","_").replace("}", "_"));
    return "(log4j jndi disabled)";
  }

  @Advice.OnMethodExit(inline = true, onThrowable = Exception.class)
  static void exit(@Advice.Enter String enter_return, @Advice.Return(readOnly = false) String ret) {
    ret = enter_return;
  }

  static AsmVisitorWrapper getVisitor() {
    return Advice.to(JndiLookup__lookup.class).on(
      ElementMatchers.named("lookup")
      .and(
        ElementMatchers.takesArguments(2).and(ElementMatchers.takesArgument(1, String.class))
      )
    );
  }

  private static final String TARGET = "org.apache.logging.log4j.core.lookup.JndiLookup";
  private static final String SUFFIX_TARGET = ".org.apache.logging.log4j.core.lookup.JndiLookup";

  public static void hook(Instrumentation inst) {
    new AgentBuilder.Default()
    .disableClassFormatChanges()
    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
    .ignore(ElementMatchers.none())
    .type(new ElementMatcher<TypeDescription>() {
      public boolean matches(TypeDescription target) {
        if (target != null) {
          String cn = target.getCanonicalName();
          if (cn != null) {
            return TARGET.equals(cn);
            /*if (cn.endsWith(TARGET)) {
              return TARGET.equals(cn) || cn.endsWith(SUFFIX_TARGET);
            }*/
          }
        }
        return false;
      }
    })
    .transform(new AgentBuilder.Transformer() {
      public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription td, ClassLoader cl, JavaModule mod) {
        try {
          return builder.visit(JndiLookup__lookup.getVisitor());
        } catch (Throwable t) {
          t.printStackTrace();
          return builder;
        }
      }
    })
    .installOn(inst);
  }


  // we assume that the entire "org.apache.logging.log4j.core" package prefix
  // could be swapped out because that is the max length prefix for
  // https://github.com/apache/logging-log4j2 from 2.0 to 2.16.0
  // (though we only really care about 2.0-2.15.0)
  private static final String LCD_TARGET = "lookup.JndiLookup";
  private static final String LCD_SUFFIX_TARGET = ".lookup.JndiLookup";

  public static void hook_deep_match(Instrumentation inst) {
    new AgentBuilder.Default()
    .disableClassFormatChanges()
    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
    .ignore(ElementMatchers.none())
    .type(new ElementMatcher<TypeDescription>() {
      public boolean matches(TypeDescription target) {
        if (target != null) {
          String cn = target.getCanonicalName();
          if (cn != null) {
            if (cn.endsWith(LCD_TARGET)) {
              if (LCD_TARGET.equals(cn) || cn.endsWith(LCD_SUFFIX_TARGET)) {
                try {
                  return is_deep_match(target);
                } catch (Throwable t) {
                  return false;
                }
              }
            }
          }
        }
        return false;
      }
    })
    .transform(new AgentBuilder.Transformer() {
      public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription td, ClassLoader cl, JavaModule mod) {
        try {
          return builder.visit(JndiLookup__lookup.getVisitor());
        } catch (Throwable t) {
          t.printStackTrace();
          return builder;
        }
      }
    })
    .installOn(inst);
  }

  // next, the @org.apache.logging.log4j.core.config.plugins.Plugin annotation
  // can be matched down to config.plugins.Plugin.

  // the base class of the JndiLookup class changes over time between
  // org.apache.logging.log4j.core.lookup.StrLookup in 2.0 and then
  // org.apache.logging.log4j.core.lookup.AbstractLookup from 2.1 on.
  // we can apply the same prefix removal, so these would be lookup.StrLookup
  // and lookup.AbstractLookup.
  // we will do more traditional in-method string operations for the prefix
  // match because this is not going to be running against every single class
  // load so simplicitly > raw performance here.
  public static boolean is_deep_match(TypeDescription target) {
    AnnotationList al = target.getDeclaredAnnotations();
    if (al.size() != 1) {
      return false;
    }
    //AnnotationDescription[] ar = al.toArray(new AnnotationDescription[]{});
    //AnnotationDescription an = ar[0];
    AnnotationDescription an = al.get(0);
    if (an == null) {
      return false;
    }

    String full_name = target.getCanonicalName();
    String base_package = target.getCanonicalName().substring(0, full_name.length()-LCD_TARGET.length());
    if (!base_package.endsWith(".") && !"".equals(base_package)) {
      //System.out.println(base_package);
      return false; // something has gone terribly wrong. bail out.
    }

    TypeDescription ant = an.getAnnotationType();
    String antn = ant.getCanonicalName();
    if (antn == null) {
      return false;
    }
    if (!(base_package + "config.plugins.Plugin").equals(antn)) {
      return false;
    }

    if (target.getModifiers() != (Modifier.PUBLIC)) {
      return false;
    }

    HashMap<String,MethodDescription.InDefinedShape> pm = new HashMap<String,MethodDescription.InDefinedShape>();
    for (MethodDescription.InDefinedShape md : ant.getDeclaredMethods()) {
      if ("lookup".equals(md.getActualName())) {
        if (md.getParameters().size() != 2) {
          continue;
        }
      }
      pm.put(md.getActualName(), md);
    }

    if (!pm.containsKey("name")) {
      return false;
    } else {
      MethodDescription.InDefinedShape md = pm.get("name");
      if (!"java.lang.String".equals(md.getReturnType().getActualName())) {
        return false;
      }
      AnnotationValue<?, ?> av = an.getValue(md);
      if (av == null || av.equals(AnnotationValue.UNDEFINED)) {
        return false;
      }
      String val = (String)av.resolve();
      if (val == null || !val.equals("jndi")) {
        return false;
      }
    }

    if (!pm.containsKey("category")) {
      return false;
    } else {
      MethodDescription.InDefinedShape md = pm.get("category");
      if (!"java.lang.String".equals(md.getReturnType().getActualName())) {
        return false;
      }
      AnnotationValue<?, ?> av = an.getValue(md);
      if (av == null || av.equals(AnnotationValue.UNDEFINED)) {
        return false;
      }
      String val = (String)av.resolve();
      if (val == null || !val.equals("Lookup")) {
        return false;
      }
    }

    if (!pm.containsKey("elementType")) {
      return false;
    } else {
      MethodDescription.InDefinedShape md = pm.get("elementType");
      if (!"java.lang.String".equals(md.getReturnType().getActualName())) {
        return false;
      }
      AnnotationValue<?, ?> av = an.getValue(md);
      if (av == null || av.equals(AnnotationValue.UNDEFINED)) {
        return false;
      }
      String val = (String)av.resolve();
      if (val == null || !val.equals("")) {
        return false;
      }
    }

    if (!pm.containsKey("printObject")) {
      return false;
    } else {
      MethodDescription.InDefinedShape md = pm.get("printObject");
      if (!"boolean".equals(md.getReturnType().getActualName())) {
        return false;
      }
      AnnotationValue<?, ?> av = an.getValue(md);
      if (av == null || av.equals(AnnotationValue.UNDEFINED)) {
        return false;
      }
      Boolean val = (Boolean)av.resolve();
      if (val == null || val != false) {
        return false;
      }
    }

    if (!pm.containsKey("deferChildren")) {
      return false;
    } else {
      MethodDescription.InDefinedShape md = pm.get("deferChildren");
      if (!"boolean".equals(md.getReturnType().getActualName())) {
        return false;
      }
      AnnotationValue<?, ?> av = an.getValue(md);
      if (av == null || av.equals(AnnotationValue.UNDEFINED)) {
        return false;
      }
      Boolean val = (Boolean)av.resolve();
      if (val == null || val != false) {
        return false;
      }
    }

    //todo: iface/subclass match
    // in log4j 2.0, JndiLookup is an implementer of org.apache.logging.log4j.core.lookup.StrLookup.
    // in 2.1+, it is a subclass of org.apache.logging.log4j.core.lookup.AbstractLookup.

    TypeList.Generic ifaces = target.getInterfaces();
    if (ifaces.size() == 1) {
      TypeDescription.Generic iface = ifaces.get(0);
      String ifacen = iface.getActualName();

      if (!(base_package + "lookup.StrLookup").equals(ifacen)) {
        return false;
      }
      TypeDescription.Generic superClass = target.getSuperClass();
      if (!"java.lang.Object".equals(superClass.getActualName())) {
        return false;
      }
    } else if (ifaces.size() == 0) {
      TypeDescription.Generic superClass = target.getSuperClass();
      String supern = superClass.getActualName();
      if (!(base_package + "lookup.AbstractLookup").equals(supern)) {
        return false;
      }
    } else {
      return false;
    }

    HashMap<String,MethodDescription.InDefinedShape> tmm = new HashMap<String,MethodDescription.InDefinedShape>();
    for (MethodDescription.InDefinedShape md : target.getDeclaredMethods()) {
      tmm.put(md.getActualName(), md);
    }
    HashMap<String,FieldDescription.InDefinedShape> tfm = new HashMap<String,FieldDescription.InDefinedShape>();
    for (FieldDescription.InDefinedShape fd : target.getDeclaredFields()) {
      tfm.put(fd.getActualName(), fd);
    }

    if (!tmm.containsKey("lookup")) {
      return false;
    } else {
      MethodDescription.InDefinedShape md = tmm.get("lookup");
      if (md.getActualModifiers() != (Modifier.PUBLIC)) {
        return false;
      }
      if (!"java.lang.String".equals(md.getReturnType().getActualName())) {
        return false;
      }
      ParameterList<ParameterDescription.InDefinedShape> pl = md.getParameters();
      /*if (pl.size() != 2) { // check hoisted
        return false;
      }*/

      // in more recent versions of log4j 2.x, the parameter names are
      // preserved in the maven JARs, but in earlier versions, they weren't,
      // so we can't rely on name checks.
      ParameterDescription.InDefinedShape p0 = pl.get(0);
      if (p0 == null) {
        return false;
      }
      String p0t = p0.getType().getTypeName();
      if (!(base_package + "LogEvent").equals(p0t)) {
        return false;
      }
      // the same issue impacts the modifiers. they come up as 0 in 2.0 to 2.13
      // and are 16 (final) only in 2.14+.
      /*if (p0.getModifiers() != (Modifier.FINAL)) {
        return false;
      }*/

      ParameterDescription.InDefinedShape p1 = pl.get(1);
      if (p1 == null) {
        return false;
      }
      String p1t = p1.getType().getTypeName();
      if (!"java.lang.String".equals(p1t)) {
        return false;
      }

    }

    if (!tmm.containsKey("convertJndiName")) {
      return false;
    } else {
      MethodDescription.InDefinedShape md = tmm.get("convertJndiName");
      if (md.getActualModifiers() != (Modifier.PRIVATE)) {
        return false;
      }
      if (!"java.lang.String".equals(md.getReturnType().getActualName())) {
        return false;
      }
      ParameterList<ParameterDescription.InDefinedShape> pl = md.getParameters();
      if (pl.size() != 1) {
        return false;
      }

      ParameterDescription.InDefinedShape p0 = pl.get(0);
      if (p0 == null) {
        return false;
      }
      String p0t = p0.getType().getTypeName();
      if (!"java.lang.String".equals(p0t)) {
        return false;
      }
    }

    if (!tfm.containsKey("CONTAINER_JNDI_RESOURCE_PATH_PREFIX")) {
      return false;
    } else {
      FieldDescription.InDefinedShape fd = tfm.get("CONTAINER_JNDI_RESOURCE_PATH_PREFIX");
      if (fd.getActualModifiers() != (Modifier.STATIC | Modifier.FINAL)) {
        return false;
      }
      if (!"java.lang.String".equals(fd.getType().getActualName())) {
        return false;
      }
    }

    return true;
  }

}
