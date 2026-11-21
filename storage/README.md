# Private storage layout

Runtime-uploaded files are deliberately excluded from Git. The resume workflow
will preserve an uploaded master source unchanged and create only versioned
derivatives beneath `resumes/customized/<job-id>/`. The original resume supplied
for this project remains an external input until it is uploaded by an
authenticated account in the resume-management milestone.
