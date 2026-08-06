* @ValidationCode : MjoxMDA1MDAyODEyOkNwMTI1MjoxNTE3NDc3MDUxNzE1OnBtYW5pdGhhOi0xOi0xOjA6MDpmYWxzZTpOL0E6REVWXzIwMTcxMS40Oi0xOi0x
* @ValidationInfo : Timestamp         : 01 Feb 2018 14:54:11
* @ValidationInfo : Encoding          : Cp1252
* @ValidationInfo : User Name         : pmanitha
* @ValidationInfo : Nb tests success  : N/A
* @ValidationInfo : Nb tests failure  : N/A
* @ValidationInfo : Rating            : N/A
* @ValidationInfo : Coverage          : N/A
* @ValidationInfo : Strict flag       : N/A
* @ValidationInfo : Bypass GateKeeper : false
* @ValidationInfo : Compiler Version  : DEV_201711.4
$PACKAGE EB.Sample
*
* Implementation of XX.SAMPLE1.XxVFtValidation
*
*
SUBROUTINE EB.V.FT.VALIDATION
    $USING EB.SystemTables
    $USING ST.Customer
    
    creditAmount = EB.SystemTables.getRNew(ST.Customer.Customer.EbCusLegalId)
    IF creditAmount > 500 THEN
        EB.SystemTables.setE('Amount greater than 500 (DS packager demo)')
    END

RETURN
