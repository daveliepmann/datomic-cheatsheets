#! /bin/bash

CMD="clj -M -m generator.generator"

set -e
set -x

#LINK_TARGET=nolinks
LINK_TARGET=links-to-datomic

# Optionally produce PDF files by running LaTeX.
# Requires latex and dvipdfm.
#PRODUCE_PDF="no"
PRODUCE_PDF="yes"

######################################################################
# Generate all four cheatsheets (HTML + LaTeX)
######################################################################
echo "Generating Datomic cheatsheets ..."
${CMD} ${LINK_TARGET}
EXIT_STATUS=$?

if [ ${EXIT_STATUS} != 0 ]
then
    echo "Exit status ${EXIT_STATUS} from ${CMD}"
    exit ${EXIT_STATUS}
fi

if [ ${PRODUCE_PDF} == "yes" ]
then
    if ! command -v latex &>/dev/null; then
        echo "WARNING: latex not found; skipping PDF generation."
    else
        for NAME in peer client async local
        do
            for PAPER in a4 usletter
            do
                for COLOR in color grey bw
                do
                    BASENAME="cheatsheet-${NAME}-${PAPER}-${COLOR}"
                    if [ -f "${BASENAME}.tex" ]
                    then
                        latex ${BASENAME}
                        dvipdfm ${BASENAME}
                        /bin/rm -f ${BASENAME}.aux ${BASENAME}.dvi ${BASENAME}.log ${BASENAME}.out
                    fi
                done
            done
        done
        /bin/mv -f *.pdf ../../pdf/ 2>/dev/null || true
    fi
fi
