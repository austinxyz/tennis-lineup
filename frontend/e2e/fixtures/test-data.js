/** Unique team name using timestamp to avoid collisions between test runs */
export function uniqueTeamName(prefix = 'E2E队伍') {
  return `${prefix}-${Date.now()}`
}

/** Sample valid players */
export const samplePlayers = {
  male: { name: 'E2E男选手', gender: 'male', utr: 5.0, verified: false },
  female: { name: 'E2E女选手', gender: 'female', utr: 4.5, verified: true },
  highUtr: { name: 'E2E高UTR', gender: 'male', utr: 10.0, verified: false },
}

/** CSV content for batch import tests */
export const csvContent = {
  /** 2 valid rows */
  valid: `name,gender,utr,verifiedDoublesUtr
张三,male,6.5,6.0
李四,female,5.0,`,

  /** 1 valid row + 1 invalid (UTR out of range) */
  partiallyInvalid: `name,gender,utr,verifiedDoublesUtr
王五,male,7.0,
InvalidUTR,male,99.0,`,

  /** All invalid */
  allInvalid: `name,gender,utr,verifiedDoublesUtr
,male,5.0,
badname,unknown,5.0,`,
}
