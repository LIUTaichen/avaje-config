package io.avaje.config;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigTest {

  @Test
  void fallbackToSystemProperty_initial() {
    System.setProperty("MySystemProp0", "bar");
    assertThat(Config.get("MySystemProp0", "foo")).isEqualTo("bar");

    // cached the initial value, still bar even when system property changed
    System.setProperty("MySystemProp0", "bazz");
    assertThat(Config.get("MySystemProp0", null)).isEqualTo("bar");

    // mutate via Config.setProperty()
    Config.setProperty("MySystemProp0", "caz");
    assertThat(Config.get("MySystemProp0")).isEqualTo("caz");

    assertThat(Config.list().of("MySystemProp0")).contains("caz");
    assertThat(Config.set().of("MySystemProp0")).contains("caz");
  }

  @Test
  void fallbackToSystemProperty_cacheInitialNullValue() {
    assertThat(Config.get("MySystemProp", null)).isNull();
    System.setProperty("MySystemProp", "hello");
    // cached the initial null so still null
    assertThat(Config.get("MySystemProp", null)).isNull();
  }

  @Test
  void fallbackToSystemProperty_cacheInitialValue() {
    assertThat(Config.get("MySystemProp2", "foo")).isEqualTo("foo");
    System.setProperty("MySystemProp2", "notFoo");
    // cached the initial value foo so still foo
    assertThat(Config.get("MySystemProp2", null)).isEqualTo("foo");
    Config.setProperty("MySystemProp2", null);
  }

  @Test
  void setProperty() {
    assertThat(Config.get("MySystemProp3", null)).isNull();
    Config.setProperty("MySystemProp3", "hello2");
    assertThat(Config.get("MySystemProp3")).isEqualTo("hello2");
  }

  @Test
  void asProperties() {
    String home = System.getProperty("user.home");

    final Properties properties = Config.asProperties();
    assertThat(properties.getProperty("myapp.fooName")).isEqualTo("Hello");
    assertThat(properties.getProperty("myapp.fooHome")).isEqualTo(home + "/config");

    assertThat(Config.get("myExternalLoader")).isEqualTo("wasExecuted");

    assertThat(properties.getProperty("config.load.systemProperties")).isEqualTo("true");
    assertThat(System.getProperty("config.load.systemProperties")).isEqualTo("true");
    assertThat(System.getProperty("myExternalLoader")).isEqualTo("wasExecuted");
    assertThat(Config.getBool("config.load.systemProperties")).isTrue();

    assertThat(properties).hasSize(7);
  }

  @Test
  void asConfiguration() {
    String home = System.getProperty("user.home");

    final Configuration configuration = Config.asConfiguration();
    assertThat(configuration.get("myapp.fooName")).isEqualTo("Hello");
    assertThat(configuration.get("myapp.fooHome")).isEqualTo(home + "/config");
  }

  @Disabled
  @Test
  void load() {
    assertThat(System.getProperty("myapp.fooName")).isNull();

    assertThat(Config.get("myapp.fooName")).isEqualTo("Hello");
    assertThat(System.getProperty("myapp.fooName")).isNull();

    Config.loadIntoSystemProperties();
    assertThat(System.getProperty("myapp.fooName")).isEqualTo("Hello");
  }

  @Test
  void get() {
    assertThat(Config.get("myapp.fooName", "junk")).isEqualTo("Hello");
  }

  @Test
  void get_withEval() {
    String home = System.getProperty("user.home");
    assertThat(Config.get("myapp.fooHome")).isEqualTo(home + "/config");
  }

  @Test
  public void get_default() {
    assertThat(Config.get("myapp.doesNotExist2", "MyDefault")).isEqualTo("MyDefault");
    assertThat(Config.get("myapp.doesNotExist2", null)).isEqualTo("MyDefault");
  }

  @Test
  void get_default_repeated_expect_returnDefaultValue() {
    assertThat(Config.get("myapp.doesNotExist3", null)).isNull();
    assertThat(Config.get("myapp.doesNotExist3", "other")).isEqualTo("other");
    assertThat(Config.get("myapp.doesNotExist3", "foo")).isEqualTo("foo");
    assertThat(Config.get("myapp.doesNotExist3", "junk")).isEqualTo("junk");
  }

  @Test
  void get_optional() {
    assertThat(Config.getOptional("myapp.doesNotExist")).isEmpty();
    assertThat(Config.getOptional("myapp.fooName")).isNotEmpty();
  }

  @Test
  void getBool_required_missing() {
    Config.setProperty("myapp.doesNotExist", null);
    assertThrows(IllegalStateException.class, () -> Config.getBool("myapp.doesNotExist"));
  }

  @Test
  void enabled_required_missing() {
    Config.setProperty("myapp.doesNotExist", null);
    assertThrows(IllegalStateException.class, () -> Config.enabled("myapp.doesNotExist"));
  }

  @Test
  void getBool_required_set() {
    assertThat(Config.getBool("myapp.activateFoo")).isTrue();
    assertThat(Config.enabled("myapp.activateFoo")).isTrue();
  }

  @Test
  void getBool() {
    assertThat(Config.getBool("myapp.activateFoo", false)).isTrue();
    assertThat(Config.enabled("myapp.activateFoo", false)).isTrue();
  }

  @Test
  void getBool_default() {
    assertThat(Config.getBool("myapp.doesNotExist", false)).isFalse();
    // default value is cached, still false
    assertThat(Config.getBool("myapp.doesNotExist", true)).isFalse();
    // can dynamically change
    Config.setProperty("myapp.doesNotExist", "true");
    assertThat(Config.getBool("myapp.doesNotExist", true)).isTrue();
  }

  @Test
  void enabled_default() {
    assertThat(Config.enabled("myapp.en.doesNotExist", false)).isFalse();
    // default value is cached, still false
    assertThat(Config.enabled("myapp.en.doesNotExist", true)).isFalse();
    // can dynamically change
    Config.setProperty("myapp.en.doesNotExist", "true");
    assertThat(Config.enabled("myapp.en.doesNotExist", true)).isTrue();
    Config.setProperty("myapp.en.doesNotExist", null);
  }

  @Test
  void getInt_required_missing() {
    assertThrows(IllegalStateException.class, () -> Config.getInt("myapp.doesNotExist"));
  }

  @Test
  void getInt_required_set() {
    assertThat(Config.getInt("myapp.bar.barRules")).isEqualTo(42);
  }

  @Test
  void getInt() {
    assertThat(Config.getInt("myapp.bar.barRules", 99)).isEqualTo(42);
  }

  @Test
  void getInt_default() {
    assertThat(Config.getInt("myapp.bar.doesNotExist", 99)).isEqualTo(99);
  }

  @Test
  void getLong_required_missing() {
    assertThrows(IllegalStateException.class, () -> Config.getLong("myapp.bar.doesNotExist"));
  }

  @Test
  void getLong_required_set() {
    assertThat(Config.getLong("myapp.bar.barRules")).isEqualTo(42L);
  }

  @Test
  void getLong() {
    assertThat(Config.getLong("myapp.bar.barRules", 99)).isEqualTo(42L);
  }

  @Test
  void getLong_default() {
    assertThat(Config.getLong("myapp.bar.doesNotExist", 99)).isEqualTo(99L);
  }

  @Test
  void getDecimal_doesNotExist() {
    assertThrows(IllegalStateException.class, () -> Config.getDecimal("myTestDecimal.doesNotExist"));
  }

  @Test
  void getDecimal_default() {
    assertThat(Config.getDecimal("myTestDecimal.doesNotExist", "10.4")).isEqualByComparingTo("10.4");
    Config.setProperty("myTestDecimal.doesNotExist", null);
  }

  @Test
  void getDecimal() {
    Config.setProperty("myTestDecimal", "14.3");
    assertThat(Config.getDecimal("myTestDecimal")).isEqualByComparingTo("14.3");
    assertThat(Config.getDecimal("myTestDecimal", "10.4")).isEqualByComparingTo("14.3");
    Config.setProperty("myTestDecimal", null);
  }

  @Test
  void getURL() throws MalformedURLException {
    Config.setProperty("myConfigUrl", "http://bana");
    assertThat(Config.getURL("myConfigUrl")).isEqualTo(new URL("http://bana"));
    assertThat(Config.getURL("myConfigUrl", "http://two")).isEqualTo(new URL("http://bana"));
    Config.setProperty("myConfigUrl", null);
  }

  @Test
  void getURI() {
    Config.setProperty("myConfigUrl", "http://bana");
    assertThat(Config.getURI("myConfigUrl")).isEqualTo(URI.create("http://bana"));
    assertThat(Config.getURI("myConfigUrl", "http://two")).isEqualTo(URI.create("http://bana"));
    Config.setProperty("myConfigUrl", null);
  }

  @Test
  void getDuration() {
    Config.setProperty("myConfigDuration", "PT10H");
    assertThat(Config.getDuration("myConfigDuration")).isEqualTo(Duration.parse("PT10H"));
    assertThat(Config.getDuration("myConfigDuration", "PT10H")).isEqualTo(Duration.parse("PT10H"));
    Config.setProperty("myConfigDuration", null);
  }

  @Test
  void getEnum_doesNotExist() {
    assertThrows(IllegalStateException.class, () -> Config.getEnum(MyTestEnum.class, "myTestEnum.doesNotExist"));
  }

  @Test
  void getEnum_default() {
    assertThat(Config.getEnum(MyTestEnum.class, "myTestEnum.doesNotExist2", MyTestEnum.C)).isEqualTo(MyTestEnum.C);
    Config.setProperty("myTestEnum.doesNotExist2", null);
  }

  @Test
  void getEnum() {
    Config.setProperty("myTestEnum", "B");
    assertThat(Config.getEnum(MyTestEnum.class, "myTestEnum")).isEqualTo(MyTestEnum.B);
    assertThat(Config.getEnum(MyTestEnum.class, "myTestEnum", MyTestEnum.C)).isEqualTo(MyTestEnum.B);
    Config.setProperty("myTestEnum", null);
  }

  enum MyTestEnum {
    A, B, C
  }

  @Test
  void onChange() {

    AtomicReference<String> ref = new AtomicReference<>();
    ref.set("initialValue");

    Config.onChange("some.key", ref::set);

    assertThat(ref.get()).isEqualTo("initialValue");
    Config.setProperty("some.key", "val1");
    assertThat(ref.get()).isEqualTo("val1");

    Config.setProperty("some.key", "val2");
    assertThat(ref.get()).isEqualTo("val2");
  }

  @Test
  void onChangeInt() {

    AtomicReference<Integer> ref = new AtomicReference<>();
    ref.set(1);

    Config.onChangeInt("some.intKey", ref::set);

    assertThat(ref.get()).isEqualTo(1);

    Config.setProperty("some.intKey", "2");
    assertThat(ref.get()).isEqualTo(2);

    Config.setProperty("some.intKey", "42");
    assertThat(ref.get()).isEqualTo(42);
  }

  @Test
  void onChangeLong() {

    AtomicReference<Long> ref = new AtomicReference<>();
    ref.set(1L);

    Config.onChangeLong("some.longKey", ref::set);

    assertThat(ref.get()).isEqualTo(1);

    Config.setProperty("some.longKey", "2");
    assertThat(ref.get()).isEqualTo(2);

    Config.setProperty("some.longKey", "42");
    assertThat(ref.get()).isEqualTo(42);
  }

  @Test
  void onChangeBool() {

    AtomicReference<Boolean> ref = new AtomicReference<>();
    ref.set(false);

    Config.onChangeBool("some.boolKey", ref::set);

    assertThat(ref.get()).isFalse();

    Config.setProperty("some.boolKey", "true");
    assertThat(ref.get()).isTrue();

    Config.setProperty("some.boolKey", "false");
    assertThat(ref.get()).isFalse();
  }
}
