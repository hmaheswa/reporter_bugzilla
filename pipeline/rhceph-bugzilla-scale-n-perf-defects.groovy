/*
    Pipeline script for executing rhceph 6.1 dashboard reports
*/
// file details
def bugzilla_api_file = "/ceph/action_report/bugzillarc"
def google_api_file = "/ceph/action_report/google_api_secret.json"
def dest_bugzilla_api_path = "/home/jenkins-build/.config/python-bugzilla"
def dest_google_api_path = "/home/jenkins-build/.gapi"

// Pipeline script entry point
node("rhel-8-medium || ceph-qe-ci") {
    stage('prepareNode') {
        if (env.WORKSPACE) { sh script: "sudo rm -rf * .venv" }
        checkout(
            scm: [
                $class: 'GitSCM',
                branches: [[name: 'main']],
                extensions: [[
                    $class: 'CleanBeforeCheckout',
                    deleteUntrackedNestedRepositories: true
                ], [
                    $class: 'WipeWorkspace'
                ], [
                    $class: 'CloneOption',
                    depth: 1,
                    noTags: true,
                    shallow: true,
                    timeout: 10,
                    reference: ''
                ]],
                userRemoteConfigs: [[
                    url: 'https://github.com/hmaheswa/reporter_bugzilla.git'
                ]]
            ],
            changelog: false,
            poll: false
        )
    }

stage("executeWorkflow") {
    sh (script: '''
    if [ ! -d '/ceph' ]; then
    sudo mkdir -p /ceph
    sudo mount -t nfs -o sec=sys,nfsvers=4.1 reesi004.ceph.redhat.com:/ /ceph
    fi
    ''')
    println 'Mounted archive volume'

    println "Install packages"
    sh (script: "rm -rf .venv")
    sh (script: "python3 -m venv .venv")
    sh (script: ".venv/bin/python3 -m pip install --upgrade pip")
    sh (script: ".venv/bin/python3 -m pip install -r requirements.txt")

    println "Adding API files"
    sh (script : "mkdir -p ${dest_bugzilla_api_path}")
    sh (script : "cp ${bugzilla_api_file} ${dest_bugzilla_api_path}")

    sh (script : "mkdir -p ${dest_google_api_path}")
    sh (script : "cp ${google_api_file} ${dest_google_api_path}")

    println "executing rhceph 6.1 dashboard report"
    sh (script: '.venv/bin/python3 scale_n_perf_defects.py "RHCS 6.1 QE - Quality Dashboard"')
    
    }
}

