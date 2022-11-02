import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.buildSteps.dotnetBuild
import jetbrains.buildServer.configs.kotlin.buildSteps.dotnetPublish
import jetbrains.buildServer.configs.kotlin.buildSteps.dotnetTest
import jetbrains.buildServer.configs.kotlin.buildSteps.nuGetInstaller
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.
VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.
To debug settings scripts in command-line, run the
    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate
command and attach your debugger to the port 8000.
To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2022.10"

project {

    buildType(Build)

    subProject(EGrate)
}

object Build : BuildType({
    name = "Build"

    params {
        param("ProGetProjectName", "eGrate")
        param("ProjectName", "eGrate")
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        dotnetPublish {
            projects = "grate/grate.csproj"
            sdk = "6"
        }
        dotnetPublish {
            name = "Build Linux"
            projects = "grate"
            configuration = "Release"
            args = "--os linux -p:PublishSingleFile=true --self-contained true"
            param("dotNetCoverage.dotCover.home.path", "%teamcity.tool.JetBrains.dotCover.CommandLineTools.DEFAULT%")
        }
        dotnetPublish {
            name = "Build MacOS (Intel)"
            projects = "Deploy_With_eGrate"
            configuration = "Release"
            args = "--os osx -p:PublishSingleFile=true --self-contained true"
            param("dotNetCoverage.dotCover.home.path", "%teamcity.tool.JetBrains.dotCover.CommandLineTools.DEFAULT%")
        }
        dotnetPublish {
            name = "Build Windows"
            projects = "grate"
            configuration = "Release"
            args = "--os osx -p:PublishSingleFile=true --self-contained true"
            param("dotNetCoverage.dotCover.home.path", "%teamcity.tool.JetBrains.dotCover.CommandLineTools.DEFAULT%")
        }
        script {
            name = "Publish to ProGet Assets"
            scriptContent = """~/Scripts/PublishToProGetFeed.sh -w "%teamcity.build.checkoutDir%" -a "%ProGetProjectName%" -p "%ProjectName%""""
        }
        script {
            name = "Add binary to bin"
            scriptContent = """cp "%teamcity.build.checkoutDir%/%ProjectName%/bin/Release/net6.0/linux-x64/publish/%ProjectName%" ~/.local/bin"""
        }
    }

    triggers {
        vcs {
        }
    }

    features {
        perfmon {
        }
    }
})


object EGrate : Project({
    name = "EGrate"

    vcsRoot(EGrate_HttpsGithubComRachelAmblerEGrateRefsHeadsMain)

    buildType(eGrate_Build)
})

object eGrate_Build : BuildType({
    name = "Build"

    vcs {
        root(EGrate_HttpsGithubComRachelAmblerEGrateRefsHeadsMain)
    }

    steps {
        nuGetInstaller {
            name = "Update Nuget packages"
            enabled = false
            toolPath = "%teamcity.tool.NuGet.CommandLine.DEFAULT%"
            projects = "grate.sln"
            updatePackages = updateParams {
            }
        }
        dotnetBuild {
            name = "Build eGrate"
            projects = "grate.sln"
            sdk = "6"
            param("dotNetCoverage.dotCover.home.path", "%teamcity.tool.JetBrains.dotCover.CommandLineTools.DEFAULT%")
        }
        dotnetTest {
            enabled = false
            projects = "grate.unittests/grate.unittests.csproj"
            sdk = "6"
        }
        dockerCommand {
            enabled = false
            commandType = build {
                source = file {
                    path = "installers/docker/Dockerfile"
                }
            }
        }
    }

    triggers {
        vcs {
        }
    }

    features {
        perfmon {
        }
    }
})

object EGrate_HttpsGithubComRachelAmblerEGrateRefsHeadsMain : GitVcsRoot({
    name = "https://github.com/RachelAmbler/eGrate#refs/heads/main"
    url = "https://github.com/RachelAmbler/eGrate"
    branch = "refs/heads/main"
    branchSpec = "refs/heads/*"
})
