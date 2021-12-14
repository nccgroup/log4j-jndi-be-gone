package trust.nccgroup.jndibegone;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class JndiLookupClassMatcher implements ElementMatcher<TypeDescription> {

  private final Config config;

  public JndiLookupClassMatcher() {
    this(Config.parse(""));
  }

  public JndiLookupClassMatcher(Config config) {
    this.config = config;
  }

  public boolean matches(TypeDescription target) {
    // TODO IMPLEMENT IT
    return "???".equals(target.getCanonicalName());
  }
}
