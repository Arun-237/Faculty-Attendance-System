import { describe, expect, it } from 'vitest'
import { emailPattern } from './signupValidation.js'

describe('Gmail signup validation', () => {
  it('accepts standard Gmail addresses and plus aliases', () => {
    expect(emailPattern.test('first.last@gmail.com')).toBe(true)
    expect(emailPattern.test('name+faculty@gmail.com')).toBe(true)
    expect(emailPattern.test('First.Last@GMAIL.COM')).toBe(true)
  })

  it('rejects malformed Gmail names and other domains', () => {
    expect(emailPattern.test('.name@gmail.com')).toBe(false)
    expect(emailPattern.test('first..last@gmail.com')).toBe(false)
    expect(emailPattern.test('first_last@gmail.com')).toBe(false)
    expect(emailPattern.test('name@example.com')).toBe(false)
  })
})
