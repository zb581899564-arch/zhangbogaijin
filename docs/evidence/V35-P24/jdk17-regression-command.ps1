mvn.cmd -q -pl jmetal-algorithm `
  "-Djacoco.skip=true" `
  "-DfailIfNoTests=false" `
  "-DargLine=--add-opens=java.base/java.lang=ALL-UNNAMED" `
  test
