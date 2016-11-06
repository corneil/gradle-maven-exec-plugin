package com.github.dkorotych.gradle.maven.cli

import spock.lang.Specification

/**
 * @author Dmitry Korotych (dkorotych at gmail dot com)
 */
class MavenCommandLineOptionsKeeperTest extends Specification {

    private static final String userHome = System.getProperty('user.home')
    private static final String tmp = System.getProperty('java.io.tmpdir')

    def "command line should be empty without any options"() {
        when:
        MavenCommandLineOptionsKeeper keeper = new MavenCommandLineOptionsKeeper()

        then:
        keeper.toCommandLine().empty
    }

    def "any method addOption not support blank option"() {
        setup:
        MavenCommandLineOptionsKeeper keeper = new MavenCommandLineOptionsKeeper()

        when:
        keeper.addOption(option, Boolean.TRUE)
        keeper.addOption(option, Boolean.FALSE)
        keeper.addOption(option, '')
        keeper.addOption(option, "")
        keeper.addOption(option, "   ")
        keeper.addOption(option, "test")
        keeper.addOption(option, new File(userHome))
        keeper.addOption(option, ['first', 'second'])
        keeper.addOption(option, [:])

        then:
        keeper.toCommandLine() == []

        where:
        option << [null, '', "", '  ', '\t\n', '\t    \n   ']
    }

    def "any method addOption not support null or empty value"() {
        setup:
        MavenCommandLineOptionsKeeper keeper = new MavenCommandLineOptionsKeeper()
        String option = '--define'

        when:
        keeper.addOption(option, (Boolean) null)
        keeper.addOption(option, (String) null)
        keeper.addOption(option, '')
        keeper.addOption(option, "")
        keeper.addOption(option, (List<String>) null)
        keeper.addOption(option, [])
        keeper.addOption(option, (File) null)
        keeper.addOption(option, (Map) null)
        keeper.addOption(option, [:])

        then:
        keeper.toCommandLine() == []
    }

    def "addOption with any unsupported value should generate UnsupportedOperationException"() {
        when:
        new MavenCommandLineOptionsKeeper().addOption(option, value)

        then:
        thrown(UnsupportedOperationException.class)

        where:
        option     | value
        'alsoMake' | (Object) null
        'object'   | new Object()
        'stream'   | new ByteArrayInputStream()
        'this'     | this
    }

    def "addOption with Boolean value"() {
        setup:
        MavenCommandLineOptionsKeeper keeper = new MavenCommandLineOptionsKeeper()

        when:
        keeper.addOption(option, value)

        then:
        commandLine == keeper.toCommandLine()

        where:
        option      | value         | commandLine
        '--offline' | true          | ['--offline']
        '--offline' | false         | []
        '--debug'   | Boolean.TRUE  | ['--debug']
        '--debug'   | Boolean.FALSE | []
    }

    def "addOption with Boolean value. Remove option if this option already exists and new value is False"() {
        setup:
        MavenCommandLineOptionsKeeper keeper = new MavenCommandLineOptionsKeeper()

        when:
        keeper.addOption("--debug", true)
        keeper.addOption(option, value)

        then:
        commandLine == keeper.toCommandLine()

        where:
        option      | value         | commandLine
        '--offline' | true          | ['--debug', '--offline']
        '--offline' | false         | ['--debug']
        '--debug'   | Boolean.TRUE  | ['--debug']
        '--debug'   | Boolean.FALSE | []
        '--debug'   | true          | ['--debug']
        '--debug'   | false         | []
    }

    def "addOption with String value"() {
        setup:
        MavenCommandLineOptionsKeeper keeper = new MavenCommandLineOptionsKeeper()

        when:
        keeper.addOption(option, value)

        then:
        commandLine == keeper.toCommandLine()

        where:
        option    | value         | commandLine
        '--debug' | "    \n\n   " | []
        '--debug' | "parameter"   | ['--debug', 'parameter']
    }

    def "addOption with String value. Replace existing option"() {
        setup:
        MavenCommandLineOptionsKeeper keeper = new MavenCommandLineOptionsKeeper()

        when:
        keeper.addOption('--offline', 'value')
        keeper.addOption(option, value)

        then:
        commandLine == keeper.toCommandLine()

        where:
        option      | value       | commandLine
        '--offline' | ''          | ['--offline', 'value']
        '--offline' | "parameter" | ['--offline', 'parameter']
    }

    def "addOption with File value"() {
        setup:
        MavenCommandLineOptionsKeeper keeper = new MavenCommandLineOptionsKeeper()

        when:
        keeper.addOption(option, value)

        then:
        commandLine == keeper.toCommandLine()

        where:
        option      | value              | commandLine
        '--offline' | new File(userHome) | ['--offline', new File(userHome).absolutePath]
        '--offline' | new File(tmp)      | ['--offline', new File(tmp).absolutePath]
    }

    def "addOption with File value. Replace existing option"() {
        setup:
        MavenCommandLineOptionsKeeper keeper = new MavenCommandLineOptionsKeeper()

        when:
        keeper.addOption('--offline', new File(userHome))
        keeper.addOption(option, value)

        then:
        commandLine == keeper.toCommandLine()

        where:
        option      | value              | commandLine
        '--offline' | new File(userHome) | ['--offline', new File(userHome).absolutePath]
        '--offline' | new File(tmp)      | ['--offline', new File(tmp).absolutePath]
    }

    def "addOption with String[] value"() {
        setup:
        MavenCommandLineOptionsKeeper keeper = new MavenCommandLineOptionsKeeper()

        when:
        keeper.addOption(option, value as List)

        then:
        commandLine == keeper.toCommandLine()

        when:
        keeper.addOption(option, value as String[])

        then:
        commandLine == keeper.toCommandLine()

        where:
        option      | value                                     | commandLine
        '--offline' | ['first']                                 | ['--offline', 'first']
        '--offline' | ['first', 'second', 'test']               | ['--offline', 'first,second,test']
        '--debug'   | ['one', null, "", '       ', 'two', '\n'] | ['--debug', 'one,two']
    }

    def "addOption with String[] value. Replace existing option"() {
        setup:
        MavenCommandLineOptionsKeeper keeper = new MavenCommandLineOptionsKeeper()

        when:
        keeper.addOption('--debug', ['first', 'second'])
        keeper.addOption(option, value as List)

        then:
        commandLine == keeper.toCommandLine()

        when:
        keeper.addOption('--debug', ['first', 'second'])
        keeper.addOption(option, value as String[])

        then:
        commandLine == keeper.toCommandLine()

        where:
        option      | value                                     | commandLine
        '--offline' | ['first']                                 | ['--debug', 'first,second', '--offline', 'first']
        '--debug'   | ['one', null, "", '       ', 'two', '\n'] | ['--debug', 'one,two']
    }

    def "addOption with Map value. Only support '--define' option with multiple values"() {
        when:
        new MavenCommandLineOptionsKeeper().addOption(option, ['first': 'value'])

        then:
        IllegalArgumentException exception = thrown(IllegalArgumentException.class)
        exception.message == 'Only support \'--define\' option with multiple values'

        where:
        option << ['--alsoMake', '--debug', '--file']
    }

    def "addOption with Map value"() {
        setup:
        MavenCommandLineOptionsKeeper keeper = new MavenCommandLineOptionsKeeper()

        when:
        keeper.addOption(option, value)

        then:
        commandLine == keeper.toCommandLine()

        where:
        option     | value                                  | commandLine
        '--define' | ['first': 'value', 'second': 'test']   | ['-Dfirst=value', '-Dsecond=test']
        '--define' | ['first': 'value', 'second': null]     | ['-Dfirst=value']
        '--define' | ['first': 'value', 'second': '    \t'] | ['-Dfirst=value']
        '--define' | ['  ': 'value', 'second': 'test']      | ['-Dsecond=test']
        '--define' | [' \t ': 'value']                      | []
    }

    def "addOption with Map value. Replace existing option"() {
        setup:
        MavenCommandLineOptionsKeeper keeper = new MavenCommandLineOptionsKeeper()

        when:
        keeper.addOption('--define', ['first': 'value', 'second': 'test'])
        keeper.addOption(option, value)

        then:
        commandLine == keeper.toCommandLine()

        where:
        option     | value                                | commandLine
        '--define' | ['first': 'value', 'second': 'test'] | ['-Dfirst=value', '-Dsecond=test']
        '--define' | ['first': 'test']                    | ['-Dfirst=test']
        '--define' | ['one': 'two']                       | ['-Done=two']
    }

    def "toCommandLine"() {
        setup:
        MavenCommandLineOptionsKeeper keeper = new MavenCommandLineOptionsKeeper()

        when:
        keeper.addOption('--offline', true)
        keeper.addOption('--thread', '1C')
        keeper.addOption('--activate-profiles', ['development'])
        keeper.addOption('--define', ['groupId': 'com.github.dkorotych', 'artifactId': 'test-maven', 'version': '1.0'])

        then:
        ['-DgroupId=com.github.dkorotych', '-DartifactId=test-maven', '-Dversion=1.0', '--offline', '--thread', '1C',
         '--activate-profiles', 'development'] == keeper.toCommandLine()
    }
}